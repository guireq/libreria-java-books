# 2. Create OAuth2 service with PKCE support

import { browser } from '$app/environment';
import { goto } from '$app/navigation';

// OAuth2 Configuration
const OAUTH_CONFIG = {
    clientId: 'web-client',
    clientSecret: 'web-secret',
    authorizationServer: 'http://localhost:9000',
    redirectUri: 'http://localhost:3000/callback',
    scopes: ['libros.read', 'libros.write'],
    state: 'xyz'
};

// PKCE Helper functions
function generateCodeVerifier(): string {
    const array = new Uint8Array(32);
    crypto.getRandomValues(array);
    return base64UrlEncode(array);
}

function base64UrlEncode(buffer: Uint8Array): string {
    const base64 = btoa(String.fromCharCode(...buffer));
    return base64.replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '');
}

async function generateCodeChallenge(verifier: string): Promise<string> {
    const encoder = new TextEncoder();
    const data = encoder.encode(verifier);
    const digest = await crypto.subtle.digest('SHA-256', data);
    return base64UrlEncode(new Uint8Array(digest));
}

export class OAuth2Service {
    private static instance: OAuth2Service;
    private accessToken: string | null = null;
    private refreshToken: string | null = null;
    private tokenExpiry: number | null = null;

    private constructor() {
        if (browser) {
            this.loadTokensFromStorage();
        }
    }

    static getInstance(): OAuth2Service {
        if (!OAuth2Service.instance) {
            OAuth2Service.instance = new OAuth2Service();
        }
        return OAuth2Service.instance;
    }

    private loadTokensFromStorage(): void {
        this.accessToken = localStorage.getItem('access_token');
        this.refreshToken = localStorage.getItem('refresh_token');
        const expiry = localStorage.getItem('token_expiry');
        this.tokenExpiry = expiry ? parseInt(expiry) : null;
    }

    private saveTokensToStorage(tokens: {
        access_token: string;
        refresh_token?: string;
        expires_in: number;
    }): void {
        this.accessToken = tokens.access_token;
        this.refreshToken = tokens.refresh_token || null;
        this.tokenExpiry = Date.now() + (tokens.expires_in * 1000);

        localStorage.setItem('access_token', tokens.access_token);
        if (tokens.refresh_token) {
            localStorage.setItem('refresh_token', tokens.refresh_token);
        }
        localStorage.setItem('token_expiry', this.tokenExpiry.toString());
    }

    async initiateLogin(): Promise<void> {
        if (!browser) return;

        const codeVerifier = generateCodeVerifier();
        const codeChallenge = await generateCodeChallenge(codeVerifier);

        // Store code verifier for later use
        sessionStorage.setItem('code_verifier', codeVerifier);

        const authUrl = new URL(`${OAUTH_CONFIG.authorizationServer}/oauth2/authorize`);
        authUrl.searchParams.set('response_type', 'code');
        authUrl.searchParams.set('client_id', OAUTH_CONFIG.clientId);
        authUrl.searchParams.set('redirect_uri', OAUTH_CONFIG.redirectUri);
        authUrl.searchParams.set('scope', OAUTH_CONFIG.scopes.join(' '));
        authUrl.searchParams.set('state', OAUTH_CONFIG.state);
        authUrl.searchParams.set('code_challenge', codeChallenge);
        authUrl.searchParams.set('code_challenge_method', 'S256');

        window.location.href = authUrl.toString();
    }

    async handleCallback(code: string, state: string): Promise<boolean> {
        if (state !== OAUTH_CONFIG.state) {
            throw new Error('Invalid state parameter');
        }

        const codeVerifier = sessionStorage.getItem('code_verifier');
        if (!codeVerifier) {
            throw new Error('Code verifier not found');
        }

        try {
            const tokenResponse = await this.exchangeCodeForTokens(code, codeVerifier);
            this.saveTokensToStorage(tokenResponse);
            sessionStorage.removeItem('code_verifier');
            return true;
        } catch (error) {
            console.error('Error exchanging code for tokens:', error);
            return false;
        }
    }

    private async exchangeCodeForTokens(code: string, codeVerifier: string): Promise<any> {
        const credentials = btoa(`${OAUTH_CONFIG.clientId}:${OAUTH_CONFIG.clientSecret}`);
        
        const response = await fetch(`${OAUTH_CONFIG.authorizationServer}/oauth2/token`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                'Authorization': `Basic ${credentials}`
            },
            body: new URLSearchParams({
                grant_type: 'authorization_code',
                code: code,
                redirect_uri: OAUTH_CONFIG.redirectUri,
                code_verifier: codeVerifier
            })
        });

        if (!response.ok) {
            throw new Error(`Token exchange failed: ${response.statusText}`);
        }

        return response.json();
    }

    async refreshAccessToken(): Promise<boolean> {
        if (!this.refreshToken) return false;

        try {
            const credentials = btoa(`${OAUTH_CONFIG.clientId}:${OAUTH_CONFIG.clientSecret}`);
            
            const response = await fetch(`${OAUTH_CONFIG.authorizationServer}/oauth2/token`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                    'Authorization': `Basic ${credentials}`
                },
                body: new URLSearchParams({
                    grant_type: 'refresh_token',
                    refresh_token: this.refreshToken
                })
            });

            if (!response.ok) return false;

            const tokens = await response.json();
            this.saveTokensToStorage(tokens);
            return true;
        } catch {
            return false;
        }
    }

    isAuthenticated(): boolean {
        return !!(this.accessToken && this.tokenExpiry && Date.now() < this.tokenExpiry);
    }

    async getValidAccessToken(): Promise<string | null> {
        if (!this.accessToken) return null;

        if (this.tokenExpiry && Date.now() >= this.tokenExpiry) {
            const refreshed = await this.refreshAccessToken();
            if (!refreshed) {
                this.logout();
                return null;
            }
        }

        return this.accessToken;
    }

    logout(): void {
        this.accessToken = null;
        this.refreshToken = null;
        this.tokenExpiry = null;

        if (browser) {
            localStorage.removeItem('access_token');
            localStorage.removeItem('refresh_token');
            localStorage.removeItem('token_expiry');
            sessionStorage.removeItem('code_verifier');
        }
    }

    getAccessToken(): string | null {
        return this.accessToken;
    }
}

# 3. Create Book API service
import { OAuth2Service } from './oauth2.js';

const API_BASE_URL = 'http://localhost:9000';

export interface Book {
    id?: number;
    titulo: string;
    autor: string;
    categoria: string;
    isbn?: string;
    fechaPublicacion?: string;
    descripcion?: string;
}

export class BookApiService {
    private oauth2Service: OAuth2Service;

    constructor() {
        this.oauth2Service = OAuth2Service.getInstance();
    }

    private async makeAuthenticatedRequest(endpoint: string, options: RequestInit = {}): Promise<Response> {
        const token = await this.oauth2Service.getValidAccessToken();
        
        if (!token) {
            throw new Error('No valid access token available');
        }

        const response = await fetch(`${API_BASE_URL}${endpoint}`, {
            ...options,
            headers: {
                ...options.headers,
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });

        if (response.status === 401) {
            // Token might be expired, try to refresh
            const refreshed = await this.oauth2Service.refreshAccessToken();
            if (refreshed) {
                const newToken = await this.oauth2Service.getValidAccessToken();
                return fetch(`${API_BASE_URL}${endpoint}`, {
                    ...options,
                    headers: {
                        ...options.headers,
                        'Authorization': `Bearer ${newToken}`,
                        'Content-Type': 'application/json'
                    }
                });
            } else {
                this.oauth2Service.logout();
                throw new Error('Authentication required');
            }
        }

        return response;
    }

    async getAllBooks(): Promise<Book[]> {
        const response = await this.makeAuthenticatedRequest('/api/book');
        
        if (!response.ok) {
            throw new Error(`Failed to fetch books: ${response.statusText}`);
        }

        return response.json();
    }

    async getBooksByAuthor(author: string): Promise<Book[]> {
        const response = await this.makeAuthenticatedRequest(`/api/book/author/${encodeURIComponent(author)}`);
        
        if (!response.ok) {
            throw new Error(`Failed to fetch books by author: ${response.statusText}`);
        }

        return response.json();
    }

    async createBook(book: Book): Promise<Book> {
        const response = await this.makeAuthenticatedRequest('/api/book', {
            method: 'POST',
            body: JSON.stringify(book)
        });

        if (!response.ok) {
            throw new Error(`Failed to create book: ${response.statusText}`);
        }

        return response.json();
    }

    async deleteBook(id: number): Promise<boolean> {
        const response = await this.makeAuthenticatedRequest(`/api/book/${id}`, {
            method: 'DELETE'
        });

        return response.ok;
    }

    async getUserInfo(): Promise<any> {
        const response = await this.makeAuthenticatedRequest('/userinfo');
        
        if (!response.ok) {
            throw new Error(`Failed to fetch user info: ${response.statusText}`);
        }

        return response.json();
    }
}

# 4. Create authentication store
import { writable } from 'svelte/store';
import { browser } from '$app/environment';
import { OAuth2Service } from '$lib/services/oauth2.js';

interface AuthState {
    isAuthenticated: boolean;
    isLoading: boolean;
    user: any | null;
    error: string | null;
}

function createAuthStore() {
    const { subscribe, set, update } = writable<AuthState>({
        isAuthenticated: false,
        isLoading: true,
        user: null,
        error: null
    });

    const oauth2Service = OAuth2Service.getInstance();

    return {
        subscribe,
        
        async init() {
            if (!browser) return;
            
            update(state => ({ ...state, isLoading: true }));
            
            try {
                const isAuth = oauth2Service.isAuthenticated();
                
                if (isAuth) {
                    // Try to get user info to validate the token
                    const { BookApiService } = await import('$lib/services/bookApi.js');
                    const bookApi = new BookApiService();
                    const userInfo = await bookApi.getUserInfo();
                    
                    set({
                        isAuthenticated: true,
                        isLoading: false,
                        user: userInfo,
                        error: null
                    });
                } else {
                    set({
                        isAuthenticated: false,
                        isLoading: false,
                        user: null,
                        error: null
                    });
                }
            } catch (error) {
                console.error('Auth init error:', error);
                oauth2Service.logout();
                set({
                    isAuthenticated: false,
                    isLoading: false,
                    user: null,
                    error: 'Authentication failed'
                });
            }
        },

        async login() {
            try {
                await oauth2Service.initiateLogin();
            } catch (error) {
                update(state => ({ 
                    ...state, 
                    error: 'Login failed: ' + (error as Error).message 
                }));
            }
        },

        async handleCallback(code: string, state: string) {
            update(state => ({ ...state, isLoading: true }));
            
            try {
                const success = await oauth2Service.handleCallback(code, state);
                
                if (success) {
                    const { BookApiService } = await import('$lib/services/bookApi.js');
                    const bookApi = new BookApiService();
                    const userInfo = await bookApi.getUserInfo();
                    
                    set({
                        isAuthenticated: true,
                        isLoading: false,
                        user: userInfo,
                        error: null
                    });
                    return true;
                } else {
                    set({
                        isAuthenticated: false,
                        isLoading: false,
                        user: null,
                        error: 'Authentication failed'
                    });
                    return false;
                }
            } catch (error) {
                set({
                    isAuthenticated: false,
                    isLoading: false,
                    user: null,
                    error: 'Authentication failed: ' + (error as Error).message
                });
                return false;
            }
        },

        logout() {
            oauth2Service.logout();
            set({
                isAuthenticated: false,
                isLoading: false,
                user: null,
                error: null
            });
        },

        clearError() {
            update(state => ({ ...state, error: null }));
        }
    };
}

export const authStore = createAuthStore();

# 5. Create callback page

import { redirect } from '@sveltejs/kit';
import type { PageLoad } from './$types';

export const load: PageLoad = async ({ url }) => {
    const code = url.searchParams.get('code');
    const state = url.searchParams.get('state');
    const error = url.searchParams.get('error');

    if (error) {
        throw redirect(302, `/?error=${encodeURIComponent(error)}`);
    }

    if (!code || !state) {
        throw redirect(302, '/?error=missing_parameters');
    }

    return {
        code,
        state
    };
};

<script lang="ts">
    import { onMount } from 'svelte';
    import { goto } from '$app/navigation';
    import { authStore } from '$lib/stores/auth.js';
    import type { PageData } from './$types';

    export let data: PageData;

    let loading = true;
    let error = '';

    onMount(async () => {
        try {
            const success = await authStore.handleCallback(data.code, data.state);
            
            if (success) {
                await goto('/books');
            } else {
                error = 'Authentication failed';
                setTimeout(() => goto('/'), 3000);
            }
        } catch (err) {
            error = 'Authentication error: ' + (err as Error).message;
            setTimeout(() => goto('/'), 3000);
        } finally {
            loading = false;
        }
    });
</script>

<div class="callback-container">
    {#if loading}
        <div class="loading">
            <div class="spinner"></div>
            <p>Processing authentication...</p>
        </div>
    {:else if error}
        <div class="error">
            <h2>Authentication Failed</h2>
            <p>{error}</p>
            <p>Redirecting to home page...</p>
        </div>
    {/if}
</div>

<style>
    .callback-container {
        display: flex;
        justify-content: center;
        align-items: center;
        min-height: 100vh;
        padding: 2rem;
    }

    .loading, .error {
        text-align: center;
        background: white;
        padding: 2rem;
        border-radius: 8px;
        box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
    }

    .spinner {
        width: 40px;
        height: 40px;
        border: 4px solid #f3f3f3;
        border-top: 4px solid #3498db;
        border-radius: 50%;
        animation: spin 1s linear infinite;
        margin: 0 auto 1rem;
    }

    @keyframes spin {
        0% { transform: rotate(0deg); }
        100% { transform: rotate(360deg); }
    }

    .error {
        background: #fee;
        color: #c33;
    }
</style>

# 6 6. Update main layout

<script lang="ts">
    import { onMount } from 'svelte';
    import { authStore } from '$lib/stores/auth.js';
    import '../app.css';

    onMount(() => {
        authStore.init();
    });
</script>

<div class="app">
    <header>
        <nav class="navbar">
            <div class="nav-brand">
                <h1>📚 Java Books Library</h1>
            </div>
            
            <div class="nav-menu">
                <a href="/" class="nav-link">Home</a>
                
                {#if $authStore.isAuthenticated}
                    <a href="/books" class="nav-link">Books</a>
                    <div class="user-menu">
                        <span class="user-name">Welcome, {$authStore.user?.preferred_username || 'User'}</span>
                        <button on:click={() => authStore.logout()} class="logout-btn">Logout</button>
                    </div>
                {:else}
                    <button on:click={() => authStore.login()} class="login-btn">Login</button>
                {/if}
            </div>
        </nav>
    </header>

    <main>
        <slot />
    </main>
</div>

<style>
    .app {
        min-height: 100vh;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    }

    .navbar {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 1rem 2rem;
        background: rgba(255, 255, 255, 0.1);
        backdrop-filter: blur(10px);
    }

    .nav-brand h1 {
        color: white;
        margin: 0;
        font-size: 1.5rem;
    }

    .nav-menu {
        display: flex;
        align-items: center;
        gap: 1rem;
    }

    .nav-link {
        color: white;
        text-decoration: none;
        padding: 0.5rem 1rem;
        border-radius: 4px;
        transition: background-color 0.2s;
    }

    .nav-link:hover {
        background-color: rgba(255, 255, 255, 0.2);
    }

    .user-menu {
        display: flex;
        align-items: center;
        gap: 1rem;
    }

    .user-name {
        color: white;
        font-weight: 500;
    }

    .login-btn, .logout-btn {
        background: rgba(255, 255, 255, 0.2);
        color: white;
        border: 1px solid rgba(255, 255, 255, 0.3);
        padding: 0.5rem 1rem;
        border-radius: 4px;
        cursor: pointer;
        transition: background-color 0.2s;
    }

    .login-btn:hover, .logout-btn:hover {
        background: rgba(255, 255, 255, 0.3);
    }

    main {
        padding: 2rem;
        min-height: calc(100vh - 80px);
    }
</style>

# 7. Create Home page

<script lang="ts">
    import { authStore } from '$lib/stores/auth.js';
    import { onMount } from 'svelte';
    import { page } from '$app/stores';

    let error = '';

    onMount(() => {
        const urlError = $page.url.searchParams.get('error');
        if (urlError) {
            error = decodeURIComponent(urlError);
        }
    });
</script>

<svelte:head>
    <title>Java Books Library</title>
</svelte:head>

<div class="home-container">
    <div class="hero">
        <h1>📚 Welcome to Java Books Library</h1>
        <p>Discover and manage your favorite Java programming books</p>
        
        {#if error}
            <div class="error-message">
                <p>⚠️ Authentication Error: {error}</p>
            </div>
        {/if}

        {#if $authStore.isLoading}
            <div class="loading">
                <p>Loading...</p>
            </div>
        {:else if $authStore.isAuthenticated}
            <div class="authenticated">
                <p>✅ Welcome back, {$authStore.user?.preferred_username || 'User'}!</p>
                <a href="/books" class="cta-button">Browse Books</a>
            </div>
        {:else}
            <div class="not-authenticated">
                <p>Sign in to access the book library</p>
                <button on:click={() => authStore.login()} class="cta-button">
                    🔐 Login with OAuth2
                </button>
            </div>
        {/if}
    </div>

    <div class="features">
        <div class="feature">
            <h3>🔍 Search Books</h3>
            <p>Find books by author, title, or category</p>
        </div>
        
        <div class="feature">
            <h3>📖 Manage Collection</h3>
            <p>Add, edit, and organize your book collection</p>
        </div>
        
        <div class="feature">
            <h3>🔐 Secure Access</h3>
            <p>OAuth2 authentication with role-based permissions</p>
        </div>
    </div>
</div>

<style>
    .home-container {
        max-width: 1200px;
        margin: 0 auto;
        padding: 2rem;
    }

    .hero {
        text-align: center;
        background: rgba(255, 255, 255, 0.1);
        backdrop-filter: blur(10px);
        padding: 4rem 2rem;
        border-radius: 12px;
        margin-bottom: 3rem;
        color: white;
    }

    .hero h1 {
        font-size: 3rem;
        margin-bottom: 1rem;
        font-weight: 700;
    }

    .hero p {
        font-size: 1.2rem;
        margin-bottom: 2rem;
        opacity: 0.9;
    }

    .error-message {
        background: rgba(255, 59, 48, 0.1);
        border: 1px solid rgba(255, 59, 48, 0.3);
        color: #ff3b30;
        padding: 1rem;
        border-radius: 8px;
        margin-bottom: 2rem;
    }

    .cta-button {
        background: rgba(255, 255, 255, 0.2);
        color: white;
        border: 2px solid rgba(255, 255, 255, 0.3);
        padding: 1rem 2rem;
        font-size: 1.1rem;
        border-radius: 8px;
        cursor: pointer;
        text-decoration: none;
        display: inline-block;
        transition: all 0.3s ease;
        font-weight: 600;
    }

    .cta-button:hover {
        background: rgba(255, 255, 255, 0.3);
        border-color: rgba(255, 255, 255, 0.5);
        transform: translateY(-2px);
    }

    .features {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
        gap: 2rem;
        margin-top: 3rem;
    }

    .feature {
        background: rgba(255, 255, 255, 0.1);
        backdrop-filter: blur(10px);
        padding: 2rem;
        border-radius: 12px;
        text-align: center;
        color: white;
    }

    .feature h3 {
        font-size: 1.5rem;
        margin-bottom: 1rem;
    }

    .feature p {
        opacity: 0.9;
        line-height: 1.6;
    }

    .loading, .authenticated, .not-authenticated {
        margin-top: 2rem;
    }
</style>

# 8. Create Books page

<script lang="ts">
    import { onMount } from 'svelte';
    import { authStore } from '$lib/stores/auth.js';
    import { BookApiService, type Book } from '$lib/services/bookApi.js';
    import { goto } from '$app/navigation';

    let bookApi = new BookApiService();
    let books: Book[] = [];
    let loading = true;
    let error = '';
    let searchAuthor = '';
    let showCreateForm = false;
    let newBook: Book = {
        titulo: '',
        autor: '',
        categoria: '',
        isbn: '',
        descripcion: ''
    };

    // Redirect if not authenticated
    $: if (!$authStore.isAuthenticated && !$authStore.isLoading) {
        goto('/');
    }

    onMount(async () => {
        if ($authStore.isAuthenticated) {
            await loadBooks();
        }
    });

    async function loadBooks() {
        try {
            loading = true;
            error = '';
            books = await bookApi.getAllBooks();
        } catch (err) {
            error = 'Failed to load books: ' + (err as Error).message;
            if (error.includes('Authentication required')) {
                authStore.logout();
                goto('/');
            }
        } finally {
            loading = false;
        }
    }

    async function searchByAuthor() {
        if (!searchAuthor.trim()) {
            await loadBooks();
            return;
        }

        try {
            loading = true;
            error = '';
            books = await bookApi.getBooksByAuthor(searchAuthor.trim());
        } catch (err) {
            error = 'Failed to search books: ' + (err as Error).message;
        } finally {
            loading = false;
        }
    }

    async function createBook() {
        if (!newBook.titulo || !newBook.autor || !newBook.categoria) {
            error = 'Please fill in all required fields';
            return;
        }

        try {
            await bookApi.createBook(newBook);
            showCreateForm = false;
            newBook = { titulo: '', autor: '', categoria: '', isbn: '', descripcion: '' };
            await loadBooks();
        } catch (err) {
            error = 'Failed to create book: ' + (err as Error).message;
        }
    }

    async function deleteBook(id: number) {
        if (!confirm('Are you sure you want to delete this book?')) return;

        try {
            await bookApi.deleteBook(id);
            await loadBooks();
        } catch (err) {
            error = 'Failed to delete book: ' + (err as Error).message;
        }
    }

    function isAdmin(): boolean {
        return $authStore.user?.authorities?.includes('ROLE_ADMIN') || false;
    }
</script>

<svelte:head>
    <title>Books - Java Books Library</title>
</svelte:head>

<div class="books-container">
    <div class="header">
        <h1>📚 Book Collection</h1>
        
        {#if isAdmin()}
            <button 
                on:click={() => showCreateForm = !showCreateForm} 
                class="add-book-btn"
            >
                ➕ Add New Book
            </button>
        {/if}
    </div>

    {#if error}
        <div class="error-message">
            <p>⚠️ {error}</p>
            <button on:click={() => error = ''} class="close-btn">✕</button>
        </div>
    {/if}

    <!-- Search Section -->
    <div class="search-section">
        <div class="search-form">
            <input
                bind:value={searchAuthor}
                placeholder="Search by author..."
                type="text"
                on:keydown={(e) => e.key === 'Enter' && searchByAuthor()}
            />
            <button on:click={searchByAuthor} class="search-btn">🔍 Search</button>
            <button on:click={loadBooks} class="clear-btn">Clear</button>
        </div>
    </div>

    <!-- Create Book Form -->
    {#if showCreateForm && isAdmin()}
        <div class="create-form">
            <h3>Create New Book</h3>
            <div class="form-grid">
                <input
                    bind:value={newBook.titulo}
                    placeholder="Title *"
                    type="text"
                    required
                />
                <input
                    bind:value={newBook.autor}
                    placeholder="Author *"
                    type="text"
                    required
                />
                <select bind:value={newBook.categoria} required>
                    <option value="">Select Category *</option>
                    <option value="PROGRAMMING">Programming</option>
                    <option value="FRAMEWORKS">Frameworks</option>
                    <option value="ARCHITECTURE">Architecture</option>
                    <option value="DATABASES">Databases</option>
                </select>
                <input
                    bind:value={newBook.isbn}
                    placeholder="ISBN"
                    type="text"
                />
            </div>
            <textarea
                bind:value={newBook.descripcion}
                placeholder="Description"
                rows="3"
            ></textarea>
            <div class="form-actions">
                <button on:click={createBook} class="create-btn">Create Book</button>
                <button on:click={() => showCreateForm = false} class="cancel-btn">Cancel</button>
            </div>
        </div>
    {/if}

    <!-- Books List -->
    {#if loading}
        <div class="loading">
            <div class="spinner"></div>
            <p>Loading books...</p>
        </div>
    {:else if books.length === 0}
        <div class="empty-state">
            <p>📖 No books found</p>
            {#if searchAuthor}
                <p>Try searching for a different author or <button on:click={loadBooks} class="link-btn">view all books</button></p>
            {/if}
        </div>
    {:else}
        <div class="books-grid">
            {#each books as book (book.id)}
                <div class="book-card">
                    <div class="book-header">
                        <h3>{book.titulo}</h3>
                        {#if isAdmin()}
                            <button 
                                on:click={() => deleteBook(book.id!)} 
                                class="delete-btn"
                                title="Delete book"
                            >
                                🗑️
                            </button>
                        {/if}
                    </div>
                    <p class="author">👤 {book.autor}</p>
                    <p class="category">🏷️ {book.categoria}</p>
                    {#if book.isbn}
                        <p class="isbn">📊 ISBN: {book.isbn}</p>
                    {/if}
                    {#if book.descripcion}
                        <p class="description">{book.descripcion}</p>
                    {/if}
                </div>
            {/each}
        </div>
    {/if}
</div>

<style>
    .books-container {
        max-width: 1200px;
        margin: 0 auto;
        padding: 2rem;
    }

    .header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 2rem;
        color: white;
    }

    .header h1 {
        margin: 0;
        font-size: 2.5rem;
    }

    .add-book-btn {
        background: rgba(76, 175, 80, 0.2);
        color: white;
        border: 2px solid rgba(76, 175, 80, 0.3);
        padding: 0.75rem 1.5rem;
        border-radius: 8px;
        cursor: pointer;
        font-weight: 600;
        transition: all 0.3s ease;
    }

    .add-book-btn:hover {
        background: rgba(76, 175, 80, 0.3);
        transform: translateY(-2px);
    }

    .error-message {
        background: rgba(255, 59, 48, 0.1);
        border: 1px solid rgba(255, 59, 48, 0.3);
        color: #ff3b30;
        padding: 1rem;
        border-radius: 8px;
        margin-bottom: 2rem;
        display: flex;
        justify-content: space-between;
        align-items: center;
    }

    .close-btn {
        background: none;
        border: none;
        color: #ff3b30;
        cursor: pointer;
        font-size: 1.2rem;
    }

    .search-section {
        background: rgba(255, 255, 255, 0.1);
        backdrop-filter: blur(10px);
        padding: 1.5rem;
        border-radius: 12px;
        margin-bottom: 2rem;
    }

    .search-form {
        display: flex;
        gap: 1rem;
        align-items: center;
    }

    .search-form input {
        flex: 1;
        padding: 0.75rem;
        border: 1px solid rgba(255, 255, 255, 0.3);
        border-radius: 6px;
        background: rgba(255, 255, 255, 0.1);
        color: white;
    }

    .search-form input::placeholder {
        color: rgba(255, 255, 255, 0.7);
    }

    .search-btn, .clear-btn {
        padding: 0.75rem 1.5rem;
        border: 1px solid rgba(255, 255, 255, 0.3);
        border-radius: 6px;
        background: rgba(255, 255, 255, 0.1);
        color: white;
        cursor: pointer;
        transition: background-color 0.2s;
    }

    .search-btn:hover, .clear-btn:hover {
        background: rgba(255, 255, 255, 0.2);
    }

    .create-form {
        background: rgba(255, 255, 255, 0.1);
        backdrop-filter: blur(10px);
        padding: 2rem;
        border-radius: 12px;
        margin-bottom: 2rem;
        color: white;
    }

    .create-form h3 {
        margin-top: 0;
        margin-bottom: 1.5rem;
    }

    .form-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
        gap: 1rem;
        margin-bottom: 1rem;
    }

    .form-grid input, .form-grid select, .create-form textarea {
        padding: 0.75rem;
        border: 1px solid rgba(255, 255, 255, 0.3);
        border-radius: 6px;
        background: rgba(255, 255, 255, 0.1);
        color: white;
    }

    .form-grid input::placeholder, .create-form textarea::placeholder {
        color: rgba(255, 255, 255, 0.7);
    }

    .create-form textarea {
        width: 100%;
        resize: vertical;
        margin-bottom: 1rem;
    }

    .form-actions {
        display: flex;
        gap: 1rem;
    }

    .create-btn, .cancel-btn {
        padding: 0.75rem 1.5rem;
        border-radius: 6px;
        cursor: pointer;
        font-weight: 600;
        transition: all 0.3s ease;
    }

    .create-btn {
        background: rgba(76, 175, 80, 0.2);
        color: white;
        border: 2px solid rgba(76, 175, 80, 0.3);
    }

    .cancel-btn {
        background: rgba(158, 158, 158, 0.2);
        color: white;
        border: 2px solid rgba(158, 158, 158, 0.3);
    }

    .loading {
        text-align: center;
        color: white;
        padding: 3rem;
    }

    .spinner {
        width: 40px;
        height: 40px;
        border: 4px solid rgba(255, 255, 255, 0.3);
        border-top: 4px solid white;
        border-radius: 50%;
        animation: spin 1s linear infinite;
        margin: 0 auto 1rem;
    }

    @keyframes spin {
        0% { transform: rotate(0deg); }
        100% { transform: rotate(360deg); }
    }

    .empty-state {
        text-align: center;
        color: white;
        padding: 3rem;
        background: rgba(255, 255, 255, 0.1);
        backdrop-filter: blur(10px);
        border-radius: 12px;
    }

    .link-btn {
        background: none;
        border: none;
        color: #87ceeb;
        text-decoration: underline;
        cursor: pointer;
    }

    .books-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
        gap: 1.5rem;
    }

    .book-card {
        background: rgba(255, 255, 255, 0.1);
        backdrop-filter: blur(10px);
        padding: 1.5rem;
        border-radius: 12px;
        color: white;
        transition: transform 0.2s ease;
    }

    .book-card:hover {
        transform: translateY(-2px);
    }

    .book-header {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        margin-bottom: 1rem;
    }

    .book-header h3 {
        margin: 0;
        font-size: 1.3rem;
        line-height: 1.3;
        flex: 1;
    }

    .delete-btn {
        background: rgba(255, 59, 48, 0.2);
        border: 1px solid rgba(255, 59, 48, 0.3);
        color: white;
        padding: 0.5rem;
        border-radius: 4px;
        cursor: pointer;
        margin-left: 1rem;
        transition: background-color 0.2s;
    }

    .delete-btn:hover {
        background: rgba(255, 59, 48, 0.3);
    }

    .author, .category, .isbn {
        margin: 0.5rem 0;
        opacity: 0.9;
        font-weight: 500;
    }

    .description {
        margin-top: 1rem;
        opacity: 0.8;
        line-height: 1.5;
        font-style: italic;
    }
</style>

# 9. Create basic CSS

* {
    box-sizing: border-box;
}

body {
    margin: 0;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen',
        'Ubuntu', 'Cantarell', 'Open Sans', 'Helvetica Neue', sans-serif;
    line-height: 1.6;
}

h1, h2, h3, h4, h5, h6 {
    margin-top: 0;
    font-weight: 600;
}

button {
    font-family: inherit;
}

input, select, textarea {
    font-family: inherit;
    font-size: inherit;
}

a {
    color: inherit;
    text-decoration: none;
}

a:hover {
    text-decoration: underline;
}

# 10 

This implementation provides:

OAuth2 Authorization Code Flow with PKCE - Secure authentication
Token management - Automatic refresh and storage
Protected routes - Redirects to login if not authenticated
Book management - View, search, create (admin only), and delete books
Role-based access - Different features for users vs admins
Responsive UI - Modern design with backdrop filters
Error handling - Comprehensive error messages and recovery
The frontend will run on http://localhost:3000 and connect to your Spring Boot OAuth2 server on http://localhost:9000. Users can authenticate using the credentials:

user / password (normal user)
admin / admin (administrator with create/delete permissions)