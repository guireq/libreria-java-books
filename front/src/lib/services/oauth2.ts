import { browser } from '$app/environment';
import { goto } from '$app/navigation';

// OAuth2 Configuration
const OAUTH_CONFIG = {
    clientId: 'web-client',
    clientSecret: 'web-secret',
    authorizationServer: 'http://localhost:9000',
    apiServer: 'http://localhost:9000', // Tu backend API
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
        // ? Ahora usamos nuestro backend proxy en lugar del Authorization Server directamente
        const response = await fetch(`${OAUTH_CONFIG.apiServer}/api/auth/token`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            credentials: 'include', // Para incluir cookies si las usas
            body: JSON.stringify({
                code: code,
                redirectUri: OAUTH_CONFIG.redirectUri,
                codeVerifier: codeVerifier
            })
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(`Token exchange failed: ${errorData.message || response.statusText}`);
        }

        return response.json();
    }

    async refreshAccessToken(): Promise<boolean> {
        if (!this.refreshToken) return false;

        try {
            // ? Usar el endpoint proxy para refresh tambi�n
            const response = await fetch(`${OAUTH_CONFIG.apiServer}/api/auth/refresh`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include',
                body: JSON.stringify({
                    refreshToken: this.refreshToken
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