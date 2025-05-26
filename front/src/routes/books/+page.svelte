<script lang="ts">
    import { onMount } from 'svelte';
    import { authStore } from '../../lib/store/auth';
    import { BookApiService, type Book } from '../../lib/services/bookApi';
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