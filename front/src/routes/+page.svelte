<script lang="ts">
    import { authStore } from '../lib/store/auth';
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