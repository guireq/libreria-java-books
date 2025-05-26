import { writable } from 'svelte/store';
import { browser } from '$app/environment';
import { OAuth2Service } from '$lib/services/oauth2';

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
                    const { BookApiService } = await import('$lib/services/bookApi');
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
                    const { BookApiService } = await import('$lib/services/bookApi');
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