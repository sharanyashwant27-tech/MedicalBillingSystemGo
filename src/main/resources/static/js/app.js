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

    // Notifications
    setupNotificationBell();
    loadNotifications();
    setInterval(loadNotifications, 60000);

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

let lastLowStockAlerts = [];
let notificationLoadGeneration = 0;
const LOW_STOCK_SEEN_KEY = 'medibill.lowStockAlertsSeenFingerprint';

function getAlertsFingerprint(alerts) {
    if (!Array.isArray(alerts) || !alerts.length) {
        return '';
    }

    return alerts
        .map(alert => {
            const id = alert.medicineId ?? alert.medicineCode ?? alert.medicineName ?? 'unknown';
            return `${String(id)}:${String(alert.currentStock ?? 0)}`;
        })
        .sort()
        .join('|');
}

function getSeenFingerprint() {
    return localStorage.getItem(LOW_STOCK_SEEN_KEY) || '';
}

function setSeenFingerprint(fingerprint) {
    if (fingerprint) {
        localStorage.setItem(LOW_STOCK_SEEN_KEY, fingerprint);
    } else {
        localStorage.removeItem(LOW_STOCK_SEEN_KEY);
    }
}

function areAlertsSeen(alerts) {
    const fingerprint = getAlertsFingerprint(alerts);
    if (!fingerprint) {
        return true;
    }
    return fingerprint === getSeenFingerprint();
}

function clearBellDisplay() {
    const wrap = document.getElementById('notificationWrap');
    const bell = document.getElementById('notificationBell');
    const badge = document.getElementById('alertCount');
    const icon = document.getElementById('notificationBellIcon');
    const message = document.getElementById('notificationMessage');

    if (badge) {
        badge.textContent = '0';
    }

    if (message) {
        message.textContent = '';
        message.title = '';
    }

    if (wrap) {
        wrap.classList.remove('has-alerts');
    }

    if (bell) {
        bell.classList.remove('has-alerts');
        bell.title = 'Low stock alerts (below 10 units)';
    }

    if (icon) {
        icon.className = 'bi bi-bell';
    }
}

function markAlertsAsSeen(alerts) {
    const fingerprint = getAlertsFingerprint(alerts);
    if (!fingerprint) {
        setSeenFingerprint('');
        clearBellDisplay();
        return;
    }

    setSeenFingerprint(fingerprint);
    clearBellDisplay();
}

function setupNotificationBell() {
    const bell = document.getElementById('notificationBell');
    const panel = document.getElementById('notificationPanel');
    const message = document.getElementById('notificationMessage');
    if (!bell || !panel) {
        return;
    }

    bell.addEventListener('click', (event) => {
        event.stopPropagation();
        toggleNotificationPanel();
    });

    if (message) {
        message.addEventListener('click', (event) => {
            event.stopPropagation();
            toggleNotificationPanel();
        });
    }

    panel.addEventListener('click', (event) => {
        event.stopPropagation();
    });

    document.addEventListener('click', (event) => {
        const wrap = document.getElementById('notificationWrap');
        if (wrap && !wrap.contains(event.target)) {
            closeNotificationPanel(true);
        }
    });
}

function closeNotificationPanel(markSeen) {
    const panel = document.getElementById('notificationPanel');
    if (!panel || !panel.classList.contains('show')) {
        return;
    }

    if (markSeen) {
        markAlertsAsSeen(lastLowStockAlerts);
    }

    panel.classList.remove('show');
}

async function toggleNotificationPanel() {
    const panel = document.getElementById('notificationPanel');
    if (!panel) {
        return;
    }

    const isOpening = !panel.classList.contains('show');

    if (!isOpening) {
        closeNotificationPanel(true);
        return;
    }

    markAlertsAsSeen(lastLowStockAlerts);
    panel.classList.add('show');
    await loadNotifications({ updateBell: false });
    markAlertsAsSeen(lastLowStockAlerts);
}

async function loadNotifications(options = {}) {
    const { updateBell = true } = options;

    if (typeof API === 'undefined') {
        return lastLowStockAlerts;
    }

    const generation = ++notificationLoadGeneration;

    try {
        const lowStockAlerts = await API.get('/api/notifications/low-stock');
        if (generation !== notificationLoadGeneration) {
            return lastLowStockAlerts;
        }

        lastLowStockAlerts = Array.isArray(lowStockAlerts) ? lowStockAlerts : [];
        renderNotificationPanel(lastLowStockAlerts);
        if (updateBell) {
            updateNotificationBell(lastLowStockAlerts);
        }
    } catch (e) {
        try {
            const data = await API.get('/api/dashboard');
            if (generation !== notificationLoadGeneration) {
                return lastLowStockAlerts;
            }

            const alerts = (data.alerts || []).filter(alert => alert.type === 'LOW_STOCK');
            lastLowStockAlerts = alerts;
            renderNotificationPanel(alerts);
            if (updateBell) {
                updateNotificationBell(alerts);
            }
        } catch (ignored) {
            // Dashboard API may not be available on login page
        }
    }

    return lastLowStockAlerts;
}

function buildBellMessage(lowStockAlerts) {
    if (!lowStockAlerts.length) {
        return '';
    }

    const first = lowStockAlerts[0];
    const name = first.medicineName || 'Medicine';
    const stock = first.currentStock ?? 0;
    const firstLine = stock <= 0
        ? `${name}: OUT OF STOCK`
        : `${name}: only ${stock} left`;

    if (lowStockAlerts.length === 1) {
        return `Low stock — ${firstLine} (< 10)`;
    }

    return `Low stock — ${lowStockAlerts.length} medicines below 10 (${firstLine})`;
}

function updateNotificationBell(lowStockAlerts) {
    if (areAlertsSeen(lowStockAlerts)) {
        clearBellDisplay();
        return;
    }

    const wrap = document.getElementById('notificationWrap');
    const bell = document.getElementById('notificationBell');
    const badge = document.getElementById('alertCount');
    const icon = document.getElementById('notificationBellIcon');
    const message = document.getElementById('notificationMessage');
    const count = lowStockAlerts.length;
    const bellMessage = buildBellMessage(lowStockAlerts);

    if (badge) {
        badge.textContent = count;
    }

    if (message) {
        message.textContent = bellMessage;
        message.title = count > 0
            ? lowStockAlerts.map(alert => alert.message || `${alert.medicineName}: ${alert.currentStock} left`).join('\n')
            : '';
    }

    if (wrap) {
        wrap.classList.toggle('has-alerts', count > 0);
    }

    if (bell) {
        bell.classList.toggle('has-alerts', count > 0);
        bell.title = count > 0 ? bellMessage : 'No low stock alerts';
    }

    if (icon) {
        icon.className = count > 0 ? 'bi bi-bell-fill' : 'bi bi-bell';
    }
}

function renderNotificationPanel(lowStockAlerts) {
    const list = document.getElementById('notificationList');
    if (!list) {
        return;
    }

    if (!lowStockAlerts.length) {
        list.innerHTML = '<p class="text-muted small mb-0 px-2 py-2"><i class="bi bi-check-circle text-success"></i> All medicines have 10 or more units in stock.</p>';
        return;
    }

    let html = `<div class="px-2 py-1 small text-danger fw-semibold">${lowStockAlerts.length} medicine(s) below 10 units</div>`;

    lowStockAlerts.slice(0, 12).forEach(alert => {
        const severityClass = alert.stockStatus === 'OUT_OF_STOCK' || alert.severity === 'danger' ? 'danger' : '';
        const statusLabel = alert.stockStatus === 'OUT_OF_STOCK' ? 'Out of stock' : 'Low stock';
        html += `
            <div class="notification-item ${severityClass}">
                <div class="notification-item-title">
                    ${escapeHtml(alert.medicineName || alert.message)}
                    <span class="badge ${severityClass === 'danger' ? 'bg-danger' : 'bg-warning text-dark'} ms-1">${statusLabel}</span>
                </div>
                <div><strong>${escapeHtml(alert.currentStock ?? 0)}</strong> units left (alert below 10)</div>
                <div class="notification-item-meta">
                    ${alert.medicineCode ? `Code: ${escapeHtml(alert.medicineCode)}` : ''}
                    ${alert.categoryName ? ` · ${escapeHtml(alert.categoryName)}` : ''}
                    ${alert.supplierName ? `<br>Supplier: ${escapeHtml(alert.supplierName)}` : ''}
                    ${alert.supplierPhone ? ` (${escapeHtml(alert.supplierPhone)})` : ''}
                    ${alert.batchNumber ? `<br>Batch: ${escapeHtml(alert.batchNumber)}` : ''}
                    ${alert.expiryDate ? ` · Expiry: ${escapeHtml(alert.expiryDate)}` : ''}
                    ${alert.rackNumber ? ` · Rack: ${escapeHtml(alert.rackNumber)}` : ''}
                </div>
            </div>`;
    });

    if (lowStockAlerts.length > 12) {
        html += `<div class="px-2 py-1 small text-muted">+ ${lowStockAlerts.length - 12} more low stock items</div>`;
    }

    list.innerHTML = html;
}

function escapeHtml(value) {
    if (value == null) {
        return '';
    }
    return String(value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

async function loadAlertCount() {
    await loadNotifications();
}
