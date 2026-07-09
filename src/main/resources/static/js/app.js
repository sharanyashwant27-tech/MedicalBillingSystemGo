document.addEventListener('DOMContentLoaded', function() {
    // Sidebar toggle
    const sidebarToggle = document.getElementById('sidebarToggle');
    const sidebar = document.getElementById('sidebar');
    if (sidebarToggle && sidebar) {
        sidebarToggle.addEventListener('click', () => sidebar.classList.toggle('show'));
    }

    // Dark mode toggle
    const darkModeToggle = document.getElementById('darkModeToggle');
    if (darkModeToggle) {
        const savedTheme = localStorage.getItem('theme') || 'light';
        document.documentElement.setAttribute('data-theme', savedTheme);
        updateDarkModeIcon(savedTheme);

        darkModeToggle.addEventListener('click', () => {
            const current = document.documentElement.getAttribute('data-theme');
            const next = current === 'dark' ? 'light' : 'dark';
            document.documentElement.setAttribute('data-theme', next);
            localStorage.setItem('theme', next);
            updateDarkModeIcon(next);
        });
    }

    // Load alerts count
    loadAlertCount();

    // Set default dates for report pages
    const startDate = document.getElementById('startDate');
    const endDate = document.getElementById('endDate');
    if (startDate && endDate) {
        const today = new Date();
        const firstDay = new Date(today.getFullYear(), today.getMonth(), 1);
        startDate.value = firstDay.toISOString().split('T')[0];
        endDate.value = today.toISOString().split('T')[0];
    }

    const purchaseDate = document.getElementById('purchaseDate');
    if (purchaseDate) {
        purchaseDate.value = new Date().toISOString().split('T')[0];
    }
});

function updateDarkModeIcon(theme) {
    const btn = document.getElementById('darkModeToggle');
    if (btn) {
        btn.innerHTML = theme === 'dark' ? '<i class="bi bi-sun"></i>' : '<i class="bi bi-moon"></i>';
    }
}

async function loadAlertCount() {
    try {
        const data = await API.get('/api/dashboard');
        const count = data.alerts ? data.alerts.length : 0;
        const badge = document.getElementById('alertCount');
        if (badge) {
            badge.textContent = count;
            badge.style.display = count > 0 ? 'inline' : 'none';
        }
    } catch (e) {
        // Dashboard API may not be available on login page
    }
}
