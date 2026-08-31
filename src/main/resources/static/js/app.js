/**
 * Application router and shell — handles page navigation,
 * sidebar rendering, and mobile responsive menu.
 */
const App = (() => {
    let currentPage = null;

    const PAGES = {
        login:       LoginPage,
        dashboard:   DashboardPage,
        catalog:     CatalogPage,
        borrows:     BorrowsPage,
        fines:       FinesPage,
        students:    StudentsPage,
        'books-mgmt': BooksMgmtPage,
        profile:     ProfilePage,
    };

    /** Navigates to a page by name. */
    function navigate(page) {
        if (!Auth.isLoggedIn() && page !== 'login') {
            page = 'login';
        }
        if (Auth.isLoggedIn() && page === 'login') {
            page = 'dashboard';
        }

        currentPage = page;
        window.location.hash = page;

        const root = document.getElementById('app');

        if (page === 'login') {
            root.innerHTML = '<div id="page-content"></div>';
            PAGES.login.render(document.getElementById('page-content'));
            return;
        }

        // App shell with sidebar
        root.innerHTML = `
            <div class="app-shell">
                <div class="mobile-header">
                    <button class="hamburger" id="menu-toggle">☰</button>
                    <span style="font-weight:600; margin-left:12px;">Central Library</span>
                </div>
                <div class="sidebar-overlay" id="sidebar-overlay"></div>
                <aside class="sidebar" id="sidebar"></aside>
                <main class="main-content" id="page-content"></main>
            </div>
        `;

        Sidebar.render(document.getElementById('sidebar'), page);

        // Mobile menu toggle
        const toggle = document.getElementById('menu-toggle');
        const sidebar = document.getElementById('sidebar');
        const overlay = document.getElementById('sidebar-overlay');

        toggle.addEventListener('click', () => {
            sidebar.classList.toggle('open');
            overlay.classList.toggle('open');
        });
        overlay.addEventListener('click', () => {
            sidebar.classList.remove('open');
            overlay.classList.remove('open');
        });

        // Render page content
        const pageModule = PAGES[page];
        if (pageModule) {
            pageModule.render(document.getElementById('page-content'));
        } else {
            document.getElementById('page-content').innerHTML = `
                <div class="page-body">
                    <div class="empty-state">
                        <div class="icon">🔍</div>
                        <h3>Page not found</h3>
                        <p>The page "${page}" does not exist.</p>
                    </div>
                </div>
            `;
        }

        // Close mobile menu after navigation
        sidebar.classList.remove('open');
        overlay.classList.remove('open');
    }

    /** Initializes the app — checks auth state and routes. */
    function init() {
        const hash = window.location.hash.slice(1);
        // If no session, go directly to login without trying dashboard (avoids 401 toast)
        if (!Auth.isLoggedIn()) {
            navigate('login');
        } else {
            navigate(hash || 'dashboard');
        }

        window.addEventListener('hashchange', () => {
            const h = window.location.hash.slice(1);
            if (h && h !== currentPage) navigate(h);
        });
    }

    return { navigate, init };
})();

// Boot the app when DOM is ready
document.addEventListener('DOMContentLoaded', () => App.init());
