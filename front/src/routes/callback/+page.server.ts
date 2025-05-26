import { redirect } from '@sveltejs/kit';
import type { PageLoad } from './$types';

export const load: PageLoad = async ({ url }) => {
    console.log('Callback URL:', url.toString());
    const code = url.searchParams.get('code');
    const state = url.searchParams.get('state');
    const error = url.searchParams.get('error');

    console.log({code, state, error});

    if (error) {
        throw redirect(302, `/?error=${encodeURIComponent(error)}`);
    }

    if (!code || !state) {
        throw redirect(302, '/?error=missing_parameters');
    }

    return { code, state };
};