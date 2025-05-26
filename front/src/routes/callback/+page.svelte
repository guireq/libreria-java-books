<script lang="ts">
    import { onMount } from 'svelte';
    import { goto } from '$app/navigation';
    import type { PageData } from './$types';
    import { authStore } from '../../lib/store/auth';

    export let data: PageData;

    let loading = true;
    let error = '';

    onMount(async () => {
        try {
            console.log('Callback data:', data);
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