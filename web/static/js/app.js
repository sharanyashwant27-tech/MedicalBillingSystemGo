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

let lastInventoryAlerts = { lowStock: [], nearExpiry: [] };
let notificationLoadGeneration = 0;
const LOW_STOCK_SEEN_KEY = 'medibill.lowStockAlertsSeenFingerprint';
const NEAR_EXPIRY_SEEN_KEY = 'medibill.nearExpiryAlertsSeenFingerprint';

function getAlertsFingerprint(alerts) {
    if (!Array.isArray(alerts) || !alerts.length) {
        return '';
    }

    return alerts
        .map(alert => {
            const id = alert.medicineId ?? alert.medicineCode ?? alert.medicineName ?? 'unknown';
            const type = alert.type ?? 'ALERT';
            const stock = alert.currentStock ?? 0;
            const expiry = alert.expiryDate ?? '';
            return `${type}:${String(id)}:${String(stock)}:${String(expiry)}`;
        })
        .sort()
        .join('|');
}

function getSeenFingerprint(seenKey) {
    return localStorage.getItem(seenKey) || '';
}

function setSeenFingerprint(seenKey, fingerprint) {
    if (fingerprint) {
        localStorage.setItem(seenKey, fingerprint);
    } else {
        localStorage.removeItem(seenKey);
    }
}

function isSectionUnread(alerts, seenKey) {
    const fingerprint = getAlertsFingerprint(alerts);
    if (!fingerprint) {
        return false;
    }
    return fingerprint !== getSeenFingerprint(seenKey);
}

function getUnreadCounts(inventoryAlerts) {
    const lowStock = inventoryAlerts.lowStock || [];
    const nearExpiry = inventoryAlerts.nearExpiry || [];
    const lowStockUnread = isSectionUnread(lowStock, LOW_STOCK_SEEN_KEY) ? lowStock.length : 0;
    const nearExpiryUnread = isSectionUnread(nearExpiry, NEAR_EXPIRY_SEEN_KEY) ? nearExpiry.length : 0;

    return {
        lowStockUnread,
        nearExpiryUnread,
        total: lowStockUnread + nearExpiryUnread
    };
}

function markSectionAsSeen(alerts, seenKey) {
    setSeenFingerprint(seenKey, getAlertsFingerprint(alerts));
}

function applyReadBellState() {
    const wrap = document.getElementById('notificationWrap');
    const bell = document.getElementById('notificationBell');
    const badge = document.getElementById('alertCount');
    const icon = document.getElementById('notificationBellIcon');
    const message = document.getElementById('notificationMessage');
    const panel = document.getElementById('notificationPanel');

    if (badge) {
        badge.textContent = '0';
    }

    if (message) {
        message.textContent = '';
        message.title = '';
        message.classList.add('is-read');
    }

    if (wrap) {
        wrap.classList.remove('has-alerts', 'has-unread-alerts');
    }

    if (bell) {
        bell.classList.remove('has-alerts', 'has-unread-alerts');
        bell.classList.add('is-read-state');
        bell.title = 'View notification history';
    }

    if (icon) {
        icon.className = 'bi bi-bell';
    }

    if (panel) {
        panel.classList.add('is-read-panel');
    }
}

function applyUnreadBellState(inventoryAlerts, unreadCounts) {
    const wrap = document.getElementById('notificationWrap');
    const bell = document.getElementById('notificationBell');
    const badge = document.getElementById('alertCount');
    const icon = document.getElementById('notificationBellIcon');
    const message = document.getElementById('notificationMessage');
    const panel = document.getElementById('notificationPanel');
    const bellMessage = buildBellMessage(inventoryAlerts, unreadCounts);
    const unreadAlerts = getUnreadAlerts(inventoryAlerts);

    if (message) {
        message.classList.remove('is-read');
    }

    if (bell) {
        bell.classList.remove('is-read-state');
    }

    if (panel) {
        panel.classList.remove('is-read-panel');
    }

    if (badge) {
        badge.textContent = unreadCounts.total;
    }

    if (message) {
        message.textContent = bellMessage;
        message.title = unreadAlerts.length
            ? unreadAlerts.map(alert => alert.message || alert.medicineName).join('\n')
            : '';
    }

    if (wrap) {
        wrap.classList.toggle('has-unread-alerts', unreadCounts.total > 0);
        wrap.classList.toggle('has-alerts', unreadCounts.total > 0);
    }

    if (bell) {
        bell.classList.toggle('has-unread-alerts', unreadCounts.total > 0);
        bell.classList.toggle('has-alerts', unreadCounts.total > 0);
        bell.title = bellMessage;
    }

    if (icon) {
        icon.className = unreadCounts.total > 0 ? 'bi bi-bell-fill' : 'bi bi-bell';
    }
}

function getUnreadAlerts(inventoryAlerts) {
    const unread = [];

    if (isSectionUnread(inventoryAlerts.lowStock || [], LOW_STOCK_SEEN_KEY)) {
        unread.push(...(inventoryAlerts.lowStock || []));
    }
    if (isSectionUnread(inventoryAlerts.nearExpiry || [], NEAR_EXPIRY_SEEN_KEY)) {
        unread.push(...(inventoryAlerts.nearExpiry || []));
    }

    return unread;
}

function markAlertsAsSeen(inventoryAlerts) {
    markSectionAsSeen(inventoryAlerts.lowStock || [], LOW_STOCK_SEEN_KEY);
    markSectionAsSeen(inventoryAlerts.nearExpiry || [], NEAR_EXPIRY_SEEN_KEY);
    updateNotificationBell(inventoryAlerts);
    renderNotificationPanel(inventoryAlerts);
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
        markAlertsAsSeen(lastInventoryAlerts);
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

    panel.classList.add('show');
    await loadNotifications({ updateBell: false });
    renderNotificationPanel(lastInventoryAlerts);
}

async function loadNotifications(options = {}) {
    const { updateBell = true } = options;

    if (typeof API === 'undefined') {
        return lastInventoryAlerts;
    }

    const generation = ++notificationLoadGeneration;

    try {
        const data = await API.get('/api/notifications/inventory-alerts');
        if (generation !== notificationLoadGeneration) {
            return lastInventoryAlerts;
        }

        lastInventoryAlerts = {
            lowStock: Array.isArray(data.lowStock) ? data.lowStock : [],
            nearExpiry: Array.isArray(data.nearExpiry) ? data.nearExpiry : []
        };
        renderNotificationPanel(lastInventoryAlerts);
        if (updateBell) {
            updateNotificationBell(lastInventoryAlerts);
        }
    } catch (e) {
        try {
            const data = await API.get('/api/dashboard');
            if (generation !== notificationLoadGeneration) {
                return lastInventoryAlerts;
            }

            const alerts = data.alerts || [];
            lastInventoryAlerts = {
                lowStock: alerts.filter(alert => alert.type === 'LOW_STOCK' || alert.type === 'OUT_OF_STOCK'),
                nearExpiry: alerts.filter(alert => alert.type === 'NEAR_EXPIRY')
            };
            renderNotificationPanel(lastInventoryAlerts);
            if (updateBell) {
                updateNotificationBell(lastInventoryAlerts);
            }
        } catch (ignored) {
            // Dashboard API may not be available on login page
        }
    }

    return lastInventoryAlerts;
}

function buildBellMessage(inventoryAlerts, unreadCounts) {
    if (!unreadCounts.total) {
        return '';
    }

    const parts = [];
    if (unreadCounts.lowStockUnread > 0) {
        parts.push(`${unreadCounts.lowStockUnread} low stock`);
    }
    if (unreadCounts.nearExpiryUnread > 0) {
        parts.push(`${unreadCounts.nearExpiryUnread} expiring soon`);
    }

    const unreadAlerts = getUnreadAlerts(inventoryAlerts);
    const preview = unreadAlerts[0]?.medicineName || 'Medicine';

    if (unreadCounts.total === 1) {
        return `Unread — ${preview} (${parts.join(', ')})`;
    }

    return `Unread — ${parts.join(', ')} (${preview})`;
}

function updateNotificationBell(inventoryAlerts) {
    const unreadCounts = getUnreadCounts(inventoryAlerts);
    const totalAlerts = (inventoryAlerts.lowStock?.length || 0) + (inventoryAlerts.nearExpiry?.length || 0);

    if (!totalAlerts || unreadCounts.total === 0) {
        applyReadBellState();
        return;
    }

    applyUnreadBellState(inventoryAlerts, unreadCounts);
}

function updateNotificationPanelHeader(inventoryAlerts) {
    const title = document.getElementById('notificationPanelTitle');
    const subtitle = document.getElementById('notificationPanelSubtitle');
    const unreadCounts = getUnreadCounts(inventoryAlerts);
    const lowStockCount = inventoryAlerts.lowStock?.length || 0;
    const nearExpiryCount = inventoryAlerts.nearExpiry?.length || 0;
    const total = lowStockCount + nearExpiryCount;

    if (title) {
        title.textContent = unreadCounts.total > 0 ? 'Unread Alerts' : 'Read Notifications';
    }

    if (subtitle) {
        if (!total) {
            subtitle.textContent = 'No low stock or near-expiry items';
        } else if (unreadCounts.total > 0) {
            subtitle.textContent = `${unreadCounts.lowStockUnread} unread low stock · ${unreadCounts.nearExpiryUnread} unread expiring`;
        } else {
            subtitle.textContent = `${total} notification(s) marked as read`;
        }
    }
}

function renderNotificationPanel(inventoryAlerts) {
    const list = document.getElementById('notificationList');
    if (!list) {
        return;
    }

    const lowStockAlerts = inventoryAlerts.lowStock || [];
    const nearExpiryAlerts = inventoryAlerts.nearExpiry || [];
    const total = lowStockAlerts.length + nearExpiryAlerts.length;

    updateNotificationPanelHeader(inventoryAlerts);

    if (!total) {
        list.innerHTML = '<p class="text-muted small mb-0 px-2 py-2"><i class="bi bi-check-circle text-success"></i> No low stock or near-expiry alerts.</p>';
        return;
    }

    let html = '<ul class="notification-panel-list">';

    if (lowStockAlerts.length) {
        const lowStockRead = !isSectionUnread(lowStockAlerts, LOW_STOCK_SEEN_KEY);
        html += `<li class="notification-section-title ${lowStockRead ? 'read-section' : 'unread-low-stock'}">${lowStockRead ? 'Low stock (read)' : 'Low stock — unread'}</li>`;
        html += renderAlertItems(lowStockAlerts, 'lowStock', lowStockRead);
    }

    if (nearExpiryAlerts.length) {
        const nearExpiryRead = !isSectionUnread(nearExpiryAlerts, NEAR_EXPIRY_SEEN_KEY);
        html += `<li class="notification-section-title ${nearExpiryRead ? 'read-section' : 'unread-near-expiry'}">${nearExpiryRead ? 'Near expiry (read)' : 'Near expiry — unread'}</li>`;
        html += renderAlertItems(nearExpiryAlerts, 'nearExpiry', nearExpiryRead);
    }

    html += '</ul>';
    list.innerHTML = html;
}

function renderAlertItems(alerts, alertType, isRead) {
    let html = '';

    alerts.forEach(alert => {
        const readClass = isRead ? ' notification-item-read' : '';
        const readBadge = isRead ? '<span class="notification-read-badge">Read</span>' : '';

        if (alertType === 'nearExpiry') {
            const daysLeft = alert.shortage ?? 0;
            const severityClass = isRead ? '' : (daysLeft <= 7 ? 'danger' : 'info');
            html += `
            <li class="notification-item ${severityClass}${readClass}">
                <div class="notification-item-title">
                    ${escapeHtml(alert.medicineName || alert.message)}${readBadge}
                    ${isRead ? '' : `<span class="badge ${daysLeft <= 7 ? 'bg-danger' : 'bg-info text-dark'} ms-1">Expiring</span>`}
                </div>
                <div><strong>${escapeHtml(daysLeft)}</strong> day(s) left · Expiry: ${escapeHtml(alert.expiryDate ?? '-')}</div>
                <div class="notification-item-meta">
                    ${alert.medicineCode ? `Code: ${escapeHtml(alert.medicineCode)}` : ''}
                    ${alert.currentStock != null ? ` · Stock: ${escapeHtml(alert.currentStock)}` : ''}
                    ${alert.batchNumber ? `<br>Batch: ${escapeHtml(alert.batchNumber)}` : ''}
                    ${alert.supplierName ? `<br>Supplier: ${escapeHtml(alert.supplierName)}` : ''}
                </div>
            </li>`;
            return;
        }

        const severityClass = isRead ? '' : (alert.stockStatus === 'OUT_OF_STOCK' || alert.severity === 'danger' ? 'danger' : '');
        const statusLabel = alert.stockStatus === 'OUT_OF_STOCK' ? 'Out of stock' : 'Low stock';
        html += `
            <li class="notification-item ${severityClass}${readClass}">
                <div class="notification-item-title">
                    ${escapeHtml(alert.medicineName || alert.message)}${readBadge}
                    ${isRead ? '' : `<span class="badge ${severityClass === 'danger' ? 'bg-danger' : 'bg-warning text-dark'} ms-1">${statusLabel}</span>`}
                </div>
                <div><strong>${escapeHtml(alert.currentStock ?? 0)}</strong> units left (alert below 10)</div>
                <div class="notification-item-meta">
                    ${alert.medicineCode ? `Code: ${escapeHtml(alert.medicineCode)}` : ''}
                    ${alert.categoryName ? ` · ${escapeHtml(alert.categoryName)}` : ''}
                    ${alert.batchNumber ? `<br>Batch: ${escapeHtml(alert.batchNumber)}` : ''}
                    ${alert.expiryDate ? ` · Expiry: ${escapeHtml(alert.expiryDate)}` : ''}
                </div>
            </li>`;
    });

    return html;
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
