import { OAuth2Service } from './oauth2';

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