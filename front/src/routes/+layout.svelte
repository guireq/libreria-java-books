<script lang="ts">
    import { onMount } from 'svelte';
    import { authStore } from '../lib/store/auth';
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