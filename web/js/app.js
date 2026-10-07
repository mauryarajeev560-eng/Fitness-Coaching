/**
 * Online Fitness Coaching Platform - Frontend Application Controller
 * Pure Vanilla JavaScript with zero external dependencies and offline SVG/Canvas charts.
 */

const AppState = {
    currentUser: null,
    activeTab: 'users', // admin: users/moderation/settings/overview/feedback | coach: plans/interaction/progress/analytics/history | user: plans/tracker/interaction/profile/history
    contacts: [],
    activeChatContactId: null,
    chatPollInterval: null
};

// --- INITIALIZATION ---
document.addEventListener('DOMContentLoaded', () => {
    // Check if user session exists in localStorage
    const savedUser = localStorage.getItem('apexfit_user');
    if (savedUser) {
        try {
            AppState.currentUser = JSON.parse(savedUser);
        } catch (e) {
            AppState.currentUser = null;
        }
    }

    if (!AppState.currentUser) {
        // Default login as Admin for quick demo evaluation
        quickLogin('admin@fitness.com', 'admin123');
    } else {
        renderApp();
    }
});

// --- TOAST NOTIFICATIONS ---
function showToast(message, isError = false) {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = 'toast' + (isError ? ' toast-error' : '');
    toast.innerHTML = `
        <span>${escapeHtml(message)}</span>
        <button style="background:transparent;border:none;color:#94a3b8;cursor:pointer;font-size:1.1rem;" onclick="this.parentElement.remove()">✕</button>
    `;
    container.appendChild(toast);

    setTimeout(() => {
        if (toast.parentElement) {
            toast.remove();
        }
    }, 4500);
}

// --- API CLIENT ---
async function apiRequest(endpoint, method = 'GET', data = null) {
    const options = {
        method: method,
        headers: {
            'Content-Type': 'application/json'
        }
    };
    if (data && (method === 'POST' || method === 'PUT')) {
        options.body = JSON.stringify(data);
    }

    try {
        const res = await fetch(endpoint, options);
        const json = await res.json();
        if (!res.ok) {
            throw new Error(json.error || 'Server request failed');
        }
        return json;
    } catch (err) {
        console.error('API Error:', err);
        throw err;
    }
}

// --- AUTHENTICATION & ROLE SWITCHING ---
async function quickLogin(email, password) {
    try {
        const res = await apiRequest('/api/auth/login', 'POST', { email, password });
        AppState.currentUser = res.data;
        localStorage.setItem('apexfit_user', JSON.stringify(AppState.currentUser));
        showToast(res.message);
        setDefaultTabForRole(AppState.currentUser.role);
        renderApp();
    } catch (err) {
        showToast(err.message, true);
    }
}

function setDefaultTabForRole(role) {
    if (role === 'ADMIN') AppState.activeTab = 'users';
    else if (role === 'COACH') AppState.activeTab = 'plans';
    else AppState.activeTab = 'plans';
}

function logout() {
    AppState.currentUser = null;
    localStorage.removeItem('apexfit_user');
    showLoginModal();
}

// --- UI RENDERING ---
function renderApp() {
    renderRoleBar();
    renderNavbar();
    renderAnnouncement();
    renderDashboard();
}

function renderRoleBar() {
    const roleBar = document.getElementById('role-bar');
    if (!roleBar) return;

    const currentEmail = AppState.currentUser ? AppState.currentUser.email : '';
    roleBar.innerHTML = `
        <div class="role-bar-left">
            <span>Active Persona:</span>
            <span class="badge">${AppState.currentUser ? AppState.currentUser.role : 'GUEST'}</span>
            <span style="color:#cbd5e1;font-weight:600;">${AppState.currentUser ? AppState.currentUser.name : 'Not Logged In'}</span>
        </div>
        <div class="role-btn-group">
            <span style="font-size:0.8rem;color:#94a3b8;align-self:center;margin-right:6px;">Switch Persona:</span>
            <button class="role-btn ${currentEmail === 'admin@fitness.com' ? 'active' : ''}" onclick="quickLogin('admin@fitness.com', 'admin123')">Admin</button>
            <button class="role-btn ${currentEmail === 'coach.marcus@fitness.com' ? 'active' : ''}" onclick="quickLogin('coach.marcus@fitness.com', 'coach123')">Coach (Marcus)</button>
            <button class="role-btn ${currentEmail === 'coach.elena@fitness.com' ? 'active' : ''}" onclick="quickLogin('coach.elena@fitness.com', 'coach123')">Coach (Elena)</button>
            <button class="role-btn ${currentEmail === 'john.doe@gmail.com' ? 'active' : ''}" onclick="quickLogin('john.doe@gmail.com', 'user123')">User (John)</button>
            <button class="role-btn ${currentEmail === 'sarah.connor@gmail.com' ? 'active' : ''}" onclick="quickLogin('sarah.connor@gmail.com', 'user123')">User (Sarah)</button>
        </div>
    `;
}

function renderNavbar() {
    const navUser = document.getElementById('nav-user');
    if (!navUser) return;

    if (AppState.currentUser) {
        const initials = AppState.currentUser.name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();
        navUser.innerHTML = `
            <div class="user-badge">
                <div class="user-avatar">${initials}</div>
                <div style="font-size:0.85rem;line-height:1.2;">
                    <div style="font-weight:700;color:#fff;">${escapeHtml(AppState.currentUser.name)}</div>
                    <div style="color:#94a3b8;font-size:0.75rem;">${AppState.currentUser.role}</div>
                </div>
            </div>
            ${AppState.currentUser.role === 'USER' ? '<button class="btn btn-outline btn-sm" onclick="openFeedbackModal()">Feedback</button>' : ''}
            <button class="btn btn-secondary btn-sm" onclick="logout()">Logout</button>
        `;
    } else {
        navUser.innerHTML = `
            <button class="btn btn-primary btn-sm" onclick="showLoginModal()">Login / Register</button>
        `;
    }
}

async function renderAnnouncement() {
    const container = document.getElementById('announcement-container');
    if (!container) return;

    try {
        const res = await apiRequest('/api/admin/settings');
        const settings = res.data || [];
        const announcement = settings.find(s => s.key === 'announcement');
        if (announcement && announcement.value) {
            container.innerHTML = `
                <div class="announcement-bar">
                    <span style="font-size:1.2rem;">📢</span>
                    <div>
                        <strong style="color:#60a5fa;">Announcement:</strong>
                        <span style="color:#e2e8f0;margin-left:6px;">${escapeHtml(announcement.value)}</span>
                    </div>
                </div>
            `;
            return;
        }
    } catch (e) {}
    container.innerHTML = '';
}

function renderDashboard() {
    const container = document.getElementById('dashboard-content');
    if (!container) return;

    if (!AppState.currentUser) {
        container.innerHTML = `<div class="panel" style="text-align:center;padding:40px;"><h3>Please log in to access the platform.</h3></div>`;
        return;
    }

    const role = AppState.currentUser.role;
    if (role === 'ADMIN') {
        renderAdminDashboard(container);
    } else if (role === 'COACH') {
        renderCoachDashboard(container);
    } else if (role === 'USER') {
        renderUserDashboard(container);
    }
}

function switchTab(tabName) {
    AppState.activeTab = tabName;
    renderDashboard();
}

// ==========================================
// 1. ADMIN DASHBOARD
// ==========================================
async function renderAdminDashboard(container) {
    container.innerHTML = `
        <div class="tab-navigation">
            <button class="tab-btn ${AppState.activeTab === 'users' ? 'active' : ''}" onclick="switchTab('users')">👥 User Management</button>
            <button class="tab-btn ${AppState.activeTab === 'moderation' ? 'active' : ''}" onclick="switchTab('moderation')">🛡️ Content Moderation</button>
            <button class="tab-btn ${AppState.activeTab === 'settings' ? 'active' : ''}" onclick="switchTab('settings')">⚙️ System Settings</button>
            <button class="tab-btn ${AppState.activeTab === 'overview' ? 'active' : ''}" onclick="switchTab('overview')">📊 Content Overview</button>
            <button class="tab-btn ${AppState.activeTab === 'feedback' ? 'active' : ''}" onclick="switchTab('feedback')">💬 User Feedback</button>
        </div>
        <div id="admin-tab-content">Loading...</div>
    `;

    const tabContainer = document.getElementById('admin-tab-content');
    if (AppState.activeTab === 'users') {
        await loadAdminUsersTab(tabContainer);
    } else if (AppState.activeTab === 'moderation') {
        await loadAdminModerationTab(tabContainer);
    } else if (AppState.activeTab === 'settings') {
        await loadAdminSettingsTab(tabContainer);
    } else if (AppState.activeTab === 'overview') {
        await loadAdminOverviewTab(tabContainer);
    } else if (AppState.activeTab === 'feedback') {
        await loadAdminFeedbackTab(tabContainer);
    }
}

async function loadAdminUsersTab(container) {
    try {
        const res = await apiRequest('/api/admin/users');
        const users = res.data || [];

        container.innerHTML = `
            <div class="panel">
                <div class="panel-header">
                    <div class="panel-title">👥 User Management</div>
                    <button class="btn btn-primary" onclick="openCreateUserModal()">+ Add New User</button>
                </div>
                <div class="filter-bar">
                    <input type="text" id="admin-user-search" class="search-input" placeholder="Search by name or email..." onkeyup="filterAdminUsers()">
                    <select id="admin-user-role-filter" class="select-input" onchange="filterAdminUsers()">
                        <option value="ALL">All Roles</option>
                        <option value="ADMIN">Administrators</option>
                        <option value="COACH">Coaches</option>
                        <option value="USER">Trainees / Users</option>
                    </select>
                    <select id="admin-user-status-filter" class="select-input" onchange="filterAdminUsers()">
                        <option value="ALL">All Statuses</option>
                        <option value="ACTIVE">Active</option>
                        <option value="INACTIVE">Inactive</option>
                    </select>
                </div>
                <div class="table-container">
                    <table id="admin-users-table">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Name</th>
                                <th>Email</th>
                                <th>Role</th>
                                <th>Status</th>
                                <th>Joined</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${renderUserRows(users)}
                        </tbody>
                    </table>
                </div>
            </div>
        `;
    } catch (err) {
        container.innerHTML = `<div class="panel" style="color:var(--danger)">Failed to load users: ${err.message}</div>`;
    }
}

function renderUserRows(users) {
    if (!users.length) return '<tr><td colspan="7" style="text-align:center;padding:24px;color:var(--text-muted)">No users found matching criteria.</td></tr>';
    return users.map(u => `
        <tr>
            <td>#${u.id}</td>
            <td><strong>${escapeHtml(u.name)}</strong></td>
            <td>${escapeHtml(u.email)}</td>
            <td><span class="badge badge-role-${u.role.toLowerCase()}">${u.role}</span></td>
            <td><span class="badge badge-${u.status.toLowerCase()}">${u.status}</span></td>
            <td>${u.created_at ? u.created_at.substring(0, 10) : ''}</td>
            <td>
                <button class="btn btn-secondary btn-sm" onclick="openEditUserModal(${JSON.stringify(u).replace(/"/g, '&quot;')})">Edit</button>
                <button class="btn btn-outline btn-sm" onclick="toggleUserStatus(${u.id}, '${u.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'}')">${u.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}</button>
                ${u.id !== 1 ? `<button class="btn btn-danger btn-sm" onclick="deleteUser(${u.id}, '${escapeHtml(u.name)}')">Delete</button>` : ''}
            </td>
        </tr>
    `).join('');
}

async function filterAdminUsers() {
    const search = document.getElementById('admin-user-search').value;
    const role = document.getElementById('admin-user-role-filter').value;
    const status = document.getElementById('admin-user-status-filter').value;

    const query = new URLSearchParams();
    if (search) query.append('search', search);
    if (role !== 'ALL') query.append('role', role);
    if (status !== 'ALL') query.append('status', status);

    try {
        const res = await apiRequest(`/api/admin/users?${query.toString()}`);
        document.querySelector('#admin-users-table tbody').innerHTML = renderUserRows(res.data || []);
    } catch (e) {}
}

async function toggleUserStatus(id, newStatus) {
    try {
        const res = await apiRequest('/api/admin/users/status', 'PUT', { id, status: newStatus });
        showToast(res.message);
        filterAdminUsers();
    } catch (err) {
        showToast(err.message, true);
    }
}

async function deleteUser(id, name) {
    if (!confirm(`Are you sure you want to permanently delete user '${name}'? This will remove all associated logs and plans.`)) return;
    try {
        const res = await apiRequest(`/api/admin/users?id=${id}`, 'DELETE');
        showToast(res.message);
        filterAdminUsers();
    } catch (err) {
        showToast(err.message, true);
    }
}

async function loadAdminModerationTab(container) {
    try {
        const res = await apiRequest('/api/admin/content');
        const plans = res.data || [];

        container.innerHTML = `
            <div class="panel">
                <div class="panel-header">
                    <div class="panel-title">🛡️ Content Moderation</div>
                    <div style="font-size:0.85rem;color:var(--text-secondary);">Review and approve coach workout plans before trainees can access them.</div>
                </div>
                <div class="filter-bar">
                    <select id="mod-status-filter" class="select-input" onchange="filterAdminContent()">
                        <option value="ALL">All Statuses</option>
                        <option value="PENDING" selected>Pending Review</option>
                        <option value="APPROVED">Approved</option>
                        <option value="REJECTED">Rejected</option>
                    </select>
                </div>
                <div class="table-container">
                    <table id="admin-mod-table">
                        <thead>
                            <tr>
                                <th>Plan ID</th>
                                <th>Title</th>
                                <th>Coach</th>
                                <th>Category</th>
                                <th>Difficulty</th>
                                <th>Duration</th>
                                <th>Status</th>
                                <th>Moderation Notes</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${renderModerationRows(plans.filter(p => p.approval_status === 'PENDING'))}
                        </tbody>
                    </table>
                </div>
            </div>
        `;
    } catch (err) {
        container.innerHTML = `<div class="panel" style="color:var(--danger)">Failed to load content: ${err.message}</div>`;
    }
}

function renderModerationRows(plans) {
    if (!plans.length) return '<tr><td colspan="9" style="text-align:center;padding:24px;color:var(--text-muted)">No plans currently pending moderation.</td></tr>';
    return plans.map(p => `
        <tr>
            <td>#${p.id}</td>
            <td><strong>${escapeHtml(p.title)}</strong></td>
            <td>${escapeHtml(p.coach_name)}</td>
            <td>${escapeHtml(p.category)}</td>
            <td><span class="badge" style="background:#334155;">${p.difficulty}</span></td>
            <td>${p.duration_weeks} Weeks</td>
            <td><span class="badge badge-${p.approval_status.toLowerCase()}">${p.approval_status}</span></td>
            <td style="max-width:200px;font-size:0.8rem;color:var(--text-secondary);">${escapeHtml(p.moderation_notes || 'None')}</td>
            <td>
                <button class="btn btn-primary btn-sm" onclick="openModerateModal(${JSON.stringify(p).replace(/"/g, '&quot;')})">Review</button>
            </td>
        </tr>
    `).join('');
}

async function filterAdminContent() {
    const status = document.getElementById('mod-status-filter').value;
    try {
        const query = status !== 'ALL' ? `?status=${status}` : '';
        const res = await apiRequest(`/api/admin/content${query}`);
        document.querySelector('#admin-mod-table tbody').innerHTML = renderModerationRows(res.data || []);
    } catch (e) {}
}

async function loadAdminSettingsTab(container) {
    try {
        const res = await apiRequest('/api/admin/settings');
        const settings = res.data || [];
        const map = {};
        settings.forEach(s => map[s.key] = s.value);

        container.innerHTML = `
            <div class="panel">
                <div class="panel-header">
                    <div class="panel-title">⚙️ System Configuration Settings</div>
                </div>
                <form id="system-settings-form" onsubmit="saveSystemSettings(event)">
                    <div class="form-grid">
                        <div class="form-group">
                            <label class="form-label">Platform Name</label>
                            <input type="text" class="form-control" name="site_name" value="${escapeHtml(map.site_name || 'ApexFit Coaching Platform')}" required>
                        </div>
                        <div class="form-group">
                            <label class="form-label">Support / Contact Email</label>
                            <input type="email" class="form-control" name="contact_email" value="${escapeHtml(map.contact_email || 'admin@apexfit.com')}" required>
                        </div>
                        <div class="form-group">
                            <label class="form-label">Max Trainees Per Coach</label>
                            <input type="number" class="form-control" name="max_users_per_coach" value="${escapeHtml(map.max_users_per_coach || '50')}" min="5" max="200" required>
                        </div>
                        <div class="form-group">
                            <label class="form-label">Default Currency</label>
                            <input type="text" class="form-control" name="default_currency" value="${escapeHtml(map.default_currency || 'USD')}" required>
                        </div>
                        <div class="form-group">
                            <label class="form-label">Allow New Trainee Registrations</label>
                            <select class="form-control" name="allow_user_registration">
                                <option value="true" ${map.allow_user_registration === 'true' ? 'selected' : ''}>Enabled (Open to Public)</option>
                                <option value="false" ${map.allow_user_registration === 'false' ? 'selected' : ''}>Disabled (Invite Only)</option>
                            </select>
                        </div>
                        <div class="form-group">
                            <label class="form-label">System Maintenance Mode</label>
                            <select class="form-control" name="maintenance_mode">
                                <option value="false" ${map.maintenance_mode === 'false' ? 'selected' : ''}>Off (Normal Operations)</option>
                                <option value="true" ${map.maintenance_mode === 'true' ? 'selected' : ''}>On (Maintenance Mode)</option>
                            </select>
                        </div>
                    </div>
                    <div class="form-group" style="margin-top:10px;">
                        <label class="form-label">Global Dashboard Announcement Banner</label>
                        <textarea class="form-control" name="announcement">${escapeHtml(map.announcement || '')}</textarea>
                    </div>
                    <button type="submit" class="btn btn-primary" style="margin-top:10px;">Save System Settings</button>
                </form>
            </div>
        `;
    } catch (err) {
        container.innerHTML = `<div class="panel" style="color:var(--danger)">Failed to load settings: ${err.message}</div>`;
    }
}

async function saveSystemSettings(e) {
    e.preventDefault();
    const form = e.target;
    const formData = new FormData(form);
    const payload = {};
    formData.forEach((val, key) => payload[key] = val);

    try {
        const res = await apiRequest('/api/admin/settings', 'POST', payload);
        showToast(res.message);
        renderAnnouncement();
    } catch (err) {
        showToast(err.message, true);
    }
}

async function loadAdminOverviewTab(container) {
    try {
        const res = await apiRequest('/api/admin/overview');
        const data = res.data || {};
        const roles = data.user_roles || {};
        const plans = data.plan_status || {};
        const overall = (data.workout_stats && data.workout_stats.overall) || {};
        const dailyTrends = (data.workout_stats && data.workout_stats.daily_trends) || [];

        container.innerHTML = `
            <div class="stats-grid">
                <div class="stat-card">
                    <div class="stat-card-info">
                        <h4>Total Registered</h4>
                        <div class="stat-value">${(roles.ADMIN || 0) + (roles.COACH || 0) + (roles.USER || 0)}</div>
                    </div>
                    <div class="stat-card-icon icon-blue">👥</div>
                </div>
                <div class="stat-card">
                    <div class="stat-card-info">
                        <h4>Active Trainees</h4>
                        <div class="stat-value">${roles.USER || 0}</div>
                    </div>
                    <div class="stat-card-icon icon-green">🏃</div>
                </div>
                <div class="stat-card">
                    <div class="stat-card-info">
                        <h4>Certified Coaches</h4>
                        <div class="stat-value">${roles.COACH || 0}</div>
                    </div>
                    <div class="stat-card-icon icon-purple">🏋️</div>
                </div>
                <div class="stat-card">
                    <div class="stat-card-info">
                        <h4>Workouts Logged</h4>
                        <div class="stat-value">${overall.total_workouts || 0}</div>
                    </div>
                    <div class="stat-card-icon icon-amber">🔥</div>
                </div>
            </div>

            <div class="two-col-grid">
                <div class="panel">
                    <div class="panel-header">
                        <div class="panel-title">👥 User Role Distribution</div>
                    </div>
                    <div class="chart-box" id="admin-role-chart"></div>
                </div>
                <div class="panel">
                    <div class="panel-header">
                        <div class="panel-title">🛡️ Workout Plans by Status</div>
                    </div>
                    <div class="chart-box" id="admin-plan-chart"></div>
                </div>
            </div>

            <div class="panel">
                <div class="panel-header">
                    <div class="panel-title">📈 Platform-Wide Workout Session Trends</div>
                </div>
                <div class="chart-box" id="admin-trend-chart"></div>
            </div>
        `;

        // Render charts
        renderDonutChart('admin-role-chart', [
            { label: 'Trainees', value: roles.USER || 0, color: '#10b981' },
            { label: 'Coaches', value: roles.COACH || 0, color: '#3b82f6' },
            { label: 'Admins', value: roles.ADMIN || 0, color: '#a855f7' }
        ]);

        renderBarChart('admin-plan-chart', [
            { label: 'Approved', value: plans.APPROVED || 0, color: '#10b981' },
            { label: 'Pending', value: plans.PENDING || 0, color: '#f59e0b' },
            { label: 'Rejected', value: plans.REJECTED || 0, color: '#ef4444' }
        ]);

        const trendPoints = dailyTrends.map(d => ({ label: d.log_date.substring(5), value: d.workout_count }));
        renderLineChart('admin-trend-chart', trendPoints, '#3b82f6', 'Workouts');

    } catch (err) {
        container.innerHTML = `<div class="panel" style="color:var(--danger)">Failed to load overview: ${err.message}</div>`;
    }
}

async function loadAdminFeedbackTab(container) {
    try {
        const res = await apiRequest('/api/admin/feedback');
        const feedback = res.data || [];

        container.innerHTML = `
            <div class="panel">
                <div class="panel-header">
                    <div class="panel-title">💬 Trainee Feedback & Reviews</div>
                </div>
                <div class="table-container">
                    <table>
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>User</th>
                                <th>Category</th>
                                <th>Subject</th>
                                <th>Rating</th>
                                <th>Message</th>
                                <th>Status</th>
                                <th>Action</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${feedback.length ? feedback.map(f => `
                                <tr>
                                    <td>#${f.id}</td>
                                    <td><strong>${escapeHtml(f.user_name)}</strong><br><span style="font-size:0.75rem;color:var(--text-muted);">${escapeHtml(f.user_email)}</span></td>
                                    <td>${escapeHtml(f.category)}</td>
                                    <td>${escapeHtml(f.subject)}</td>
                                    <td style="color:#f59e0b;">${'★'.repeat(f.rating)}${'☆'.repeat(5 - f.rating)}</td>
                                    <td style="max-width:250px;font-size:0.85rem;">${escapeHtml(f.message)}</td>
                                    <td><span class="badge ${f.status === 'REVIEWED' ? 'badge-approved' : 'badge-pending'}">${f.status}</span></td>
                                    <td>
                                        ${f.status !== 'REVIEWED' ? `<button class="btn btn-primary btn-sm" onclick="markFeedbackReviewed(${f.id})">Mark Reviewed</button>` : '<span style="color:var(--primary);font-size:0.8rem;">✓ Resolved</span>'}
                                    </td>
                                </tr>
                            `).join('') : '<tr><td colspan="8" style="text-align:center;padding:20px;color:var(--text-muted)">No feedback received yet.</td></tr>'}
                        </tbody>
                    </table>
                </div>
            </div>
        `;
    } catch (err) {
        container.innerHTML = `<div class="panel" style="color:var(--danger)">Failed to load feedback: ${err.message}</div>`;
    }
}

async function markFeedbackReviewed(id) {
    try {
        const res = await apiRequest('/api/admin/feedback', 'POST', { id, status: 'REVIEWED' });
        showToast(res.message);
        loadAdminFeedbackTab(document.getElementById('admin-tab-content'));
    } catch (err) {
        showToast(err.message, true);
    }
}

// ==========================================
// 2. COACH DASHBOARD
// ==========================================
async function renderCoachDashboard(container) {
    container.innerHTML = `
        <div class="tab-navigation">
            <button class="tab-btn ${AppState.activeTab === 'plans' ? 'active' : ''}" onclick="switchTab('plans')">📋 Workout Plan Management</button>
            <button class="tab-btn ${AppState.activeTab === 'interaction' ? 'active' : ''}" onclick="switchTab('interaction')">💬 User Interaction</button>
            <button class="tab-btn ${AppState.activeTab === 'progress' ? 'active' : ''}" onclick="switchTab('progress')">📊 Progress Tracking</button>
            <button class="tab-btn ${AppState.activeTab === 'analytics' ? 'active' : ''}" onclick="switchTab('analytics')">📈 Plan Analytics</button>
            <button class="tab-btn ${AppState.activeTab === 'history' ? 'active' : ''}" onclick="switchTab('history')">📜 Interaction History</button>
        </div>
        <div id="coach-tab-content">Loading...</div>
    `;

    const tabContainer = document.getElementById('coach-tab-content');
    if (AppState.activeTab === 'plans') {
        await loadCoachPlansTab(tabContainer);
    } else if (AppState.activeTab === 'interaction') {
        await loadCoachInteractionTab(tabContainer);
    } else if (AppState.activeTab === 'progress') {
        await loadCoachProgressTab(tabContainer);
    } else if (AppState.activeTab === 'analytics') {
        await loadCoachAnalyticsTab(tabContainer);
    } else if (AppState.activeTab === 'history') {
        await loadCoachHistoryTab(tabContainer);
    }
}

async function loadCoachPlansTab(container) {
    try {
        const res = await apiRequest(`/api/coach/plans?coachId=${AppState.currentUser.id}`);
        const plans = res.data || [];

        container.innerHTML = `
            <div class="panel">
                <div class="panel-header">
                    <div class="panel-title">📋 Workout Plans Created by You</div>
                    <button class="btn btn-primary" onclick="openCreatePlanModal()">+ Create Workout Plan</button>
                </div>
                <div class="table-container">
                    <table>
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Plan Title</th>
                                <th>Category</th>
                                <th>Difficulty</th>
                                <th>Duration</th>
                                <th>Status</th>
                                <th>Enrolled Users</th>
                                <th>Exercises</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${plans.length ? plans.map(p => `
                                <tr>
                                    <td>#${p.id}</td>
                                    <td><strong>${escapeHtml(p.title)}</strong></td>
                                    <td>${escapeHtml(p.category)}</td>
                                    <td><span class="badge" style="background:#334155;">${p.difficulty}</span></td>
                                    <td>${p.duration_weeks} Weeks</td>
                                    <td><span class="badge badge-${p.approval_status.toLowerCase()}">${p.approval_status}</span></td>
                                    <td>${p.enrolled_count || 0} Trainees</td>
                                    <td>${(p.exercises && p.exercises.length) || 0} Exercises</td>
                                    <td>
                                        <button class="btn btn-secondary btn-sm" onclick="viewPlanExercises(${p.id})">View Exercises</button>
                                        <button class="btn btn-danger btn-sm" onclick="deletePlan(${p.id})">Delete</button>
                                    </td>
                                </tr>
                            `).join('') : '<tr><td colspan="9" style="text-align:center;padding:24px;color:var(--text-muted)">You have not created any workout plans yet. Click "+ Create Workout Plan" to get started!</td></tr>'}
                        </tbody>
                    </table>
                </div>
            </div>
        `;
    } catch (err) {
        container.innerHTML = `<div class="panel" style="color:var(--danger)">Failed to load plans: ${err.message}</div>`;
    }
}

async function deletePlan(planId) {
    if (!confirm('Are you sure you want to delete this workout plan?')) return;
    try {
        const res = await apiRequest(`/api/coach/plans?id=${planId}`, 'DELETE');
        showToast(res.message);
        loadCoachPlansTab(document.getElementById('coach-tab-content'));
    } catch (err) {
        showToast(err.message, true);
    }
}

async function loadCoachInteractionTab(container) {
    try {
        const res = await apiRequest(`/api/coach/interactions?coachId=${AppState.currentUser.id}`);
        AppState.contacts = res.data || [];

        container.innerHTML = `
            <div class="panel" style="padding:0;overflow:hidden;">
                <div class="chat-container">
                    <div class="contacts-list" id="chat-contacts-list">
                        <div style="padding:14px;border-bottom:1px solid var(--dark-border);font-weight:700;color:#fff;">
                            💬 Active Trainees (${AppState.contacts.length})
                        </div>
                        ${renderContactsList()}
                    </div>
                    <div class="chat-pane" id="chat-pane">
                        <div style="flex:1;display:flex;align-items:center;justify-content:center;color:var(--text-muted);">
                            Select a trainee from the list to view conversation and provide feedback.
                        </div>
                    </div>
                </div>
            </div>
        `;

        if (AppState.contacts.length > 0) {
            selectContact(AppState.contacts[0].contact_id, AppState.contacts[0].contact_name);
        }
    } catch (err) {
        container.innerHTML = `<div class="panel" style="color:var(--danger)">Failed to load interactions: ${err.message}</div>`;
    }
}

function renderContactsList() {
    if (!AppState.contacts.length) {
        return '<div style="padding:20px;text-align:center;color:var(--text-muted);font-size:0.85rem;">No trainee messages yet.</div>';
    }
    return AppState.contacts.map(c => `
        <div class="contact-item ${AppState.activeChatContactId === c.contact_id ? 'active' : ''}" onclick="selectContact(${c.contact_id}, '${escapeHtml(c.contact_name)}')">
            <div class="user-avatar" style="background:#10b981;">${c.contact_name[0]}</div>
            <div class="contact-info">
                <h5>${escapeHtml(c.contact_name)}</h5>
                <p>${escapeHtml(c.last_message || 'No messages')}</p>
            </div>
            ${c.unread_count > 0 ? `<span class="badge badge-approved" style="margin-left:auto;">${c.unread_count}</span>` : ''}
        </div>
    `).join('');
}

async function selectContact(contactId, contactName) {
    AppState.activeChatContactId = contactId;
    const list = document.getElementById('chat-contacts-list');
    if (list) list.innerHTML = `<div style="padding:14px;border-bottom:1px solid var(--dark-border);font-weight:700;color:#fff;">💬 Active Trainees (${AppState.contacts.length})</div>` + renderContactsList();

    const pane = document.getElementById('chat-pane');
    if (!pane) return;

    pane.innerHTML = `
        <div class="chat-header">
            <div>
                <strong>${escapeHtml(contactName)}</strong>
                <span class="badge badge-role-user" style="margin-left:8px;">Trainee</span>
            </div>
            <button class="btn btn-outline btn-sm" onclick="viewTraineeProgressModal(${contactId}, '${escapeHtml(contactName)}')">View Progress Report</button>
        </div>
        <div class="chat-messages" id="chat-messages-box">Loading conversation...</div>
        <form class="chat-input-bar" onsubmit="sendCoachMessage(event)">
            <input type="text" id="chat-input-text" class="search-input" placeholder="Type coaching advice or response..." required autocomplete="off">
            <button type="submit" class="btn btn-primary">Send</button>
        </form>
    `;

    await loadConversationMessages(contactId);
}

async function loadConversationMessages(contactId) {
    const box = document.getElementById('chat-messages-box');
    if (!box) return;

    try {
        const res = await apiRequest(`/api/coach/messages?coachId=${AppState.currentUser.id}&userId=${contactId}`);
        const messages = res.data || [];

        if (!messages.length) {
            box.innerHTML = '<div style="text-align:center;color:var(--text-muted);margin:auto;">No messages yet. Start the conversation below!</div>';
            return;
        }

        box.innerHTML = messages.map(m => {
            const isMe = m.sender_id === AppState.currentUser.id;
            return `
                <div class="chat-bubble ${isMe ? 'sent' : 'received'}">
                    <div>${escapeHtml(m.message_text)}</div>
                    <span class="msg-time">${m.sent_at ? m.sent_at.substring(11, 16) : ''}</span>
                </div>
            `;
        }).join('');
        box.scrollTop = box.scrollHeight;
    } catch (e) {}
}

async function sendCoachMessage(e) {
    e.preventDefault();
    const input = document.getElementById('chat-input-text');
    const text = input.value.trim();
    if (!text || !AppState.activeChatContactId) return;

    try {
        const res = await apiRequest('/api/coach/messages', 'POST', {
            senderId: AppState.currentUser.id,
            receiverId: AppState.activeChatContactId,
            messageText: text
        });
        input.value = '';
        showToast(res.message);
        loadConversationMessages(AppState.activeChatContactId);
    } catch (err) {
        showToast(err.message, true);
    }
}

async function loadCoachProgressTab(container) {
    try {
        const res = await apiRequest(`/api/coach/trainees?coachId=${AppState.currentUser.id}`);
        const trainees = res.data || [];

        container.innerHTML = `
            <div class="panel">
                <div class="panel-header">
                    <div class="panel-title">📊 Track Trainee Fitness Progress</div>
                </div>
                <div class="table-container">
                    <table>
                        <thead>
                            <tr>
                                <th>Trainee</th>
                                <th>Enrolled Plan</th>
                                <th>Goal</th>
                                <th>Current Weight</th>
                                <th>Target Weight</th>
                                <th>Workouts Logged</th>
                                <th>Total Calories</th>
                                <th>Last Active</th>
                                <th>Progress Report</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${trainees.length ? trainees.map(t => `
                                <tr>
                                    <td><strong>${escapeHtml(t.user_name)}</strong><br><span style="font-size:0.75rem;color:var(--text-muted);">${escapeHtml(t.user_email)}</span></td>
                                    <td><span class="badge" style="background:#1e3a8a;color:#93c5fd;">${escapeHtml(t.active_plan_title || 'None')}</span></td>
                                    <td>${escapeHtml(t.fitness_goal || 'General')}</td>
                                    <td><strong>${t.current_weight || '--'} kg</strong></td>
                                    <td>${t.target_weight || '--'} kg</td>
                                    <td>${t.workouts_logged} Sessions</td>
                                    <td>${t.total_calories} kcal</td>
                                    <td>${t.last_workout_date || 'No logs yet'}</td>
                                    <td>
                                        <button class="btn btn-primary btn-sm" onclick="viewTraineeProgressModal(${t.user_id}, '${escapeHtml(t.user_name)}')">View Charts</button>
                                    </td>
                                </tr>
                            `).join('') : '<tr><td colspan="9" style="text-align:center;padding:24px;color:var(--text-muted)">No trainees currently enrolled in your workout plans.</td></tr>'}
                        </tbody>
                    </table>
                </div>
            </div>
            <div id="trainee-selected-analytics"></div>
        `;
    } catch (err) {
        container.innerHTML = `<div class="panel" style="color:var(--danger)">Failed to load trainees: ${err.message}</div>`;
    }
}

async function loadCoachAnalyticsTab(container) {
    try {
        const res = await apiRequest(`/api/coach/analytics?coachId=${AppState.currentUser.id}`);
        const analytics = res.data || [];

        container.innerHTML = `
            <div class="panel">
                <div class="panel-header">
                    <div class="panel-title">📈 Plan Effectiveness & User Engagement Analytics</div>
                </div>
                <div class="two-col-grid" style="margin-bottom:20px;">
                    <div>
                        <h4 style="font-size:0.95rem;color:var(--text-secondary);margin-bottom:8px;">Trainee Enrollment per Plan</h4>
                        <div class="chart-box" id="coach-enrollment-chart"></div>
                    </div>
                    <div>
                        <h4 style="font-size:0.95rem;color:var(--text-secondary);margin-bottom:8px;">Total Workout Sessions Completed</h4>
                        <div class="chart-box" id="coach-sessions-chart"></div>
                    </div>
                </div>
                <div class="table-container">
                    <table>
                        <thead>
                            <tr>
                                <th>Plan ID</th>
                                <th>Plan Title</th>
                                <th>Category</th>
                                <th>Difficulty</th>
                                <th>Active Trainees</th>
                                <th>Total Workouts Logged</th>
                                <th>Avg Calorie Burn / Session</th>
                                <th>Status</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${analytics.length ? analytics.map(a => `
                                <tr>
                                    <td>#${a.id}</td>
                                    <td><strong>${escapeHtml(a.title)}</strong></td>
                                    <td>${escapeHtml(a.category)}</td>
                                    <td>${a.difficulty}</td>
                                    <td><strong style="color:var(--primary);">${a.total_enrolled}</strong></td>
                                    <td>${a.total_workouts_logged}</td>
                                    <td>${Math.round(a.avg_calories || 0)} kcal</td>
                                    <td><span class="badge badge-${a.approval_status.toLowerCase()}">${a.approval_status}</span></td>
                                </tr>
                            `).join('') : '<tr><td colspan="8" style="text-align:center;padding:20px;color:var(--text-muted)">No analytics data available yet.</td></tr>'}
                        </tbody>
                    </table>
                </div>
            </div>
        `;

        if (analytics.length) {
            const enrollData = analytics.map(a => ({ label: a.title.substring(0, 14), value: a.total_enrolled, color: '#10b981' }));
            const sessData = analytics.map(a => ({ label: a.title.substring(0, 14), value: a.total_workouts_logged, color: '#3b82f6' }));
            renderBarChart('coach-enrollment-chart', enrollData);
            renderBarChart('coach-sessions-chart', sessData);
        }
    } catch (err) {
        container.innerHTML = `<div class="panel" style="color:var(--danger)">Failed to load analytics: ${err.message}</div>`;
    }
}

async function loadCoachHistoryTab(container) {
    try {
        const res = await apiRequest(`/api/coach/interaction-history?coachId=${AppState.currentUser.id}`);
        const history = res.data || [];

        container.innerHTML = `
            <div class="panel">
                <div class="panel-header">
                    <div class="panel-title">📜 Communication & Interaction History Log</div>
                </div>
                <div class="table-container">
                    <table>
                        <thead>
                            <tr>
                                <th>Timestamp</th>
                                <th>Sender</th>
                                <th>Receiver</th>
                                <th>Message Content</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${history.length ? history.map(h => `
                                <tr>
                                    <td style="white-space:nowrap;font-size:0.8rem;color:var(--text-muted);">${h.sent_at}</td>
                                    <td><strong>${escapeHtml(h.sender_name)}</strong> <span class="badge badge-role-${h.sender_role.toLowerCase()}">${h.sender_role}</span></td>
                                    <td><strong>${escapeHtml(h.receiver_name)}</strong> <span class="badge badge-role-${h.receiver_role.toLowerCase()}">${h.receiver_role}</span></td>
                                    <td>${escapeHtml(h.message_text)}</td>
                                </tr>
                            `).join('') : '<tr><td colspan="4" style="text-align:center;padding:24px;color:var(--text-muted)">No interaction history recorded yet.</td></tr>'}
                        </tbody>
                    </table>
                </div>
            </div>
        `;
    } catch (err) {
        container.innerHTML = `<div class="panel" style="color:var(--danger)">Failed to load history: ${err.message}</div>`;
    }
}

// ==========================================
// 3. USER DASHBOARD
// ==========================================
async function renderUserDashboard(container) {
    container.innerHTML = `
        <div class="tab-navigation">
            <button class="tab-btn ${AppState.activeTab === 'plans' ? 'active' : ''}" onclick="switchTab('plans')">🏋️ Workout Plans</button>
            <button class="tab-btn ${AppState.activeTab === 'tracker' ? 'active' : ''}" onclick="switchTab('tracker')">📈 Fitness Progress Tracker</button>
            <button class="tab-btn ${AppState.activeTab === 'interaction' ? 'active' : ''}" onclick="switchTab('interaction')">💬 Coach Interaction</button>
            <button class="tab-btn ${AppState.activeTab === 'history' ? 'active' : ''}" onclick="switchTab('history')">📜 Workout History & Badges</button>
            <button class="tab-btn ${AppState.activeTab === 'profile' ? 'active' : ''}" onclick="switchTab('profile')">👤 Profile Management</button>
        </div>
        <div id="user-tab-content">Loading...</div>
    `;

    const tabContainer = document.getElementById('user-tab-content');
    if (AppState.activeTab === 'plans') {
        await loadUserPlansTab(tabContainer);
    } else if (AppState.activeTab === 'tracker') {
        await loadUserTrackerTab(tabContainer);
    } else if (AppState.activeTab === 'interaction') {
        await loadUserInteractionTab(tabContainer);
    } else if (AppState.activeTab === 'history') {
        await loadUserHistoryTab(tabContainer);
    } else if (AppState.activeTab === 'profile') {
        await loadUserProfileTab(tabContainer);
    }
}

async function loadUserPlansTab(container) {
    try {
        const [activeRes, catalogRes] = await Promise.all([
            apiRequest(`/api/user/plans/active?userId=${AppState.currentUser.id}`),
            apiRequest(`/api/user/plans`)
        ]);

        const activePlans = activeRes.data || [];
        const catalogPlans = catalogRes.data || [];
        const enrolledIds = new Set(activePlans.map(p => p.id));

        container.innerHTML = `
            <!-- Active Workout Plans -->
            <div class="panel">
                <div class="panel-header">
                    <div class="panel-title">⚡ Your Active Enrolled Plans (${activePlans.length})</div>
                </div>
                ${activePlans.length ? `
                    <div class="plans-grid">
                        ${activePlans.map(p => `
                            <div class="plan-card" style="border-color:var(--primary);">
                                <div>
                                    <div class="plan-card-header">
                                        <span class="badge badge-approved">Active Routine</span>
                                        <span class="badge" style="background:#334155;">${p.difficulty}</span>
                                    </div>
                                    <h3 class="plan-card-title">${escapeHtml(p.title)}</h3>
                                    <div style="font-size:0.8rem;color:var(--primary);margin-bottom:8px;">Coach: ${escapeHtml(p.coach_name)}</div>
                                    <p class="plan-card-desc">${escapeHtml(p.description)}</p>
                                    <div class="plan-meta">
                                        <span>📅 ${p.duration_weeks} Weeks</span>
                                        <span>🏷️ ${escapeHtml(p.category)}</span>
                                        <span>📋 ${(p.exercises && p.exercises.length) || 0} Exercises</span>
                                    </div>
                                </div>
                                <div style="display:flex;gap:8px;margin-top:12px;">
                                    <button class="btn btn-primary btn-sm" style="flex:1;" onclick="viewPlanExercises(${p.id})">Daily Exercises</button>
                                    <button class="btn btn-outline btn-sm" onclick="openLogWorkoutModal(${p.id}, '${escapeHtml(p.title)}')">+ Log Session</button>
                                    <button class="btn btn-danger btn-sm" onclick="unenrollPlan(${p.id})">Drop</button>
                                </div>
                            </div>
                        `).join('')}
                    </div>
                ` : `
                    <div style="padding:24px;text-align:center;color:var(--text-muted);">
                        You are not currently enrolled in any workout plan. Explore available plans below and click <strong>"Enroll Now"</strong> to begin!
                    </div>
                `}
            </div>

            <!-- Available Workout Plans Catalog -->
            <div class="panel">
                <div class="panel-header">
                    <div class="panel-title">📚 Available Workout Plans Catalog</div>
                </div>
                <div class="plans-grid">
                    ${catalogPlans.map(p => {
                        const isEnrolled = enrolledIds.has(p.id);
                        return `
                            <div class="plan-card">
                                <div>
                                    <div class="plan-card-header">
                                        <span class="badge" style="background:#1e293b;border:1px solid var(--dark-border);color:#38bdf8;">${escapeHtml(p.category)}</span>
                                        <span class="badge" style="background:#334155;">${p.difficulty}</span>
                                    </div>
                                    <h3 class="plan-card-title">${escapeHtml(p.title)}</h3>
                                    <div style="font-size:0.8rem;color:var(--text-secondary);margin-bottom:8px;">Coach: <strong>${escapeHtml(p.coach_name)}</strong></div>
                                    <p class="plan-card-desc">${escapeHtml(p.description)}</p>
                                    <div class="plan-meta">
                                        <span>⏱️ ${p.duration_weeks} Weeks</span>
                                        <span>👥 ${p.enrolled_count || 0} Enrolled</span>
                                    </div>
                                </div>
                                <div style="display:flex;gap:8px;margin-top:12px;">
                                    <button class="btn btn-secondary btn-sm" style="flex:1;" onclick="viewPlanExercises(${p.id})">View Routine</button>
                                    ${isEnrolled ? `
                                        <button class="btn btn-outline btn-sm" disabled style="opacity:0.6;">Enrolled ✓</button>
                                    ` : `
                                        <button class="btn btn-primary btn-sm" onclick="enrollPlan(${p.id})">Enroll Now</button>
                                    `}
                                </div>
                            </div>
                        `;
                    }).join('')}
                </div>
            </div>
        `;
    } catch (err) {
        container.innerHTML = `<div class="panel" style="color:var(--danger)">Failed to load plans: ${err.message}</div>`;
    }
}

async function enrollPlan(planId) {
    try {
        const res = await apiRequest('/api/user/plans/enroll', 'POST', {
            userId: AppState.currentUser.id,
            planId: planId
        });
        showToast(res.message);
        loadUserPlansTab(document.getElementById('user-tab-content'));
    } catch (err) {
        showToast(err.message, true);
    }
}

async function unenrollPlan(planId) {
    if (!confirm('Are you sure you want to stop following this plan?')) return;
    try {
        const res = await apiRequest('/api/user/plans/unenroll', 'POST', {
            userId: AppState.currentUser.id,
            planId: planId
        });
        showToast(res.message);
        loadUserPlansTab(document.getElementById('user-tab-content'));
    } catch (err) {
        showToast(err.message, true);
    }
}

async function loadUserTrackerTab(container) {
    try {
        const res = await apiRequest(`/api/user/progress/metrics?userId=${AppState.currentUser.id}`);
        const data = res.data || {};
        const totals = data.totals || {};
        const profile = data.profile || {};
        const history = data.history || [];

        container.innerHTML = `
            <div class="stats-grid">
                <div class="stat-card">
                    <div class="stat-card-info">
                        <h4>Completed Workouts</h4>
                        <div class="stat-value">${totals.total_workouts || 0}</div>
                    </div>
                    <div class="stat-card-icon icon-green">🏆</div>
                </div>
                <div class="stat-card">
                    <div class="stat-card-info">
                        <h4>Total Calories Burned</h4>
                        <div class="stat-value">${totals.total_calories || 0} <span style="font-size:0.9rem;font-weight:400;color:var(--text-muted);">kcal</span></div>
                    </div>
                    <div class="stat-card-icon icon-amber">🔥</div>
                </div>
                <div class="stat-card">
                    <div class="stat-card-info">
                        <h4>Total Training Time</h4>
                        <div class="stat-value">${totals.total_duration || 0} <span style="font-size:0.9rem;font-weight:400;color:var(--text-muted);">mins</span></div>
                    </div>
                    <div class="stat-card-icon icon-blue">⏱️</div>
                </div>
                <div class="stat-card">
                    <div class="stat-card-info">
                        <h4>Current / Target Weight</h4>
                        <div class="stat-value">${profile.current_weight || '--'} / ${profile.target_weight || '--'} <span style="font-size:0.9rem;font-weight:400;color:var(--text-muted);">kg</span></div>
                    </div>
                    <div class="stat-card-icon icon-purple">⚖️</div>
                </div>
            </div>

            <div class="panel">
                <div class="panel-header">
                    <div class="panel-title">📈 Fitness Progress Graphs</div>
                    <button class="btn btn-primary" onclick="openLogWorkoutModal()">+ Log Workout Session</button>
                </div>
                <div class="two-col-grid">
                    <div>
                        <h4 style="font-size:0.95rem;color:var(--text-secondary);margin-bottom:8px;">Body Weight Progression (kg)</h4>
                        <div class="chart-box" id="user-weight-chart"></div>
                    </div>
                    <div>
                        <h4 style="font-size:0.95rem;color:var(--text-secondary);margin-bottom:8px;">Calorie Burn per Workout (kcal)</h4>
                        <div class="chart-box" id="user-calories-chart"></div>
                    </div>
                </div>
            </div>
        `;

        if (history.length) {
            const weightPoints = history.filter(h => h.weight > 0).map(h => ({ label: h.log_date.substring(5), value: h.weight }));
            const calPoints = history.map(h => ({ label: h.log_date.substring(5), value: h.calories_burned }));
            renderLineChart('user-weight-chart', weightPoints, '#10b981', 'Weight (kg)');
            renderBarChart('user-calories-chart', calPoints.map(c => ({ label: c.label, value: c.value, color: '#f59e0b' })));
        } else {
            document.getElementById('user-weight-chart').innerHTML = '<div style="color:var(--text-muted);">No workout logs yet. Log your first workout to see graphs!</div>';
            document.getElementById('user-calories-chart').innerHTML = '<div style="color:var(--text-muted);">No workout logs yet.</div>';
        }

    } catch (err) {
        container.innerHTML = `<div class="panel" style="color:var(--danger)">Failed to load tracker: ${err.message}</div>`;
    }
}

async function loadUserInteractionTab(container) {
    try {
        const coachesRes = await apiRequest('/api/user/coaches');
        const coaches = coachesRes.data || [];

        container.innerHTML = `
            <div class="panel" style="padding:0;overflow:hidden;">
                <div class="chat-container">
                    <div class="contacts-list" id="user-coaches-list">
                        <div style="padding:14px;border-bottom:1px solid var(--dark-border);font-weight:700;color:#fff;">
                            🏋️ Certified Coaches (${coaches.length})
                        </div>
                        ${coaches.map(c => `
                            <div class="contact-item ${AppState.activeChatContactId === c.id ? 'active' : ''}" onclick="selectUserCoach(${c.id}, '${escapeHtml(c.name)}')">
                                <div class="user-avatar" style="background:#3b82f6;">${c.name[0]}</div>
                                <div class="contact-info">
                                    <h5>${escapeHtml(c.name)}</h5>
                                    <p>${escapeHtml(c.bio || 'Coach')}</p>
                                </div>
                            </div>
                        `).join('')}
                    </div>
                    <div class="chat-pane" id="user-chat-pane">
                        <div style="flex:1;display:flex;align-items:center;justify-content:center;color:var(--text-muted);">
                            Select a coach to ask for guidance, discuss your routine, or get lifting tips.
                        </div>
                    </div>
                </div>
            </div>
        `;

        if (coaches.length > 0) {
            selectUserCoach(coaches[0].id, coaches[0].name);
        }
    } catch (err) {
        container.innerHTML = `<div class="panel" style="color:var(--danger)">Failed to load coaches: ${err.message}</div>`;
    }
}

async function selectUserCoach(coachId, coachName) {
    AppState.activeChatContactId = coachId;
    const pane = document.getElementById('user-chat-pane');
    if (!pane) return;

    pane.innerHTML = `
        <div class="chat-header">
            <div>
                <strong>${escapeHtml(coachName)}</strong>
                <span class="badge badge-role-coach" style="margin-left:8px;">Fitness Coach</span>
            </div>
            <span style="font-size:0.8rem;color:var(--primary);">● Online</span>
        </div>
        <div class="chat-messages" id="user-messages-box">Loading messages...</div>
        <form class="chat-input-bar" onsubmit="sendUserMessage(event)">
            <input type="text" id="user-input-text" class="search-input" placeholder="Ask your coach anything about workouts, diet, or recovery..." required autocomplete="off">
            <button type="submit" class="btn btn-primary">Send</button>
        </form>
    `;

    await loadUserMessages(coachId);
}

async function loadUserMessages(coachId) {
    const box = document.getElementById('user-messages-box');
    if (!box) return;

    try {
        const res = await apiRequest(`/api/user/messages?userId=${AppState.currentUser.id}&coachId=${coachId}`);
        const messages = res.data || [];

        if (!messages.length) {
            box.innerHTML = '<div style="text-align:center;color:var(--text-muted);margin:auto;">Send a message to start communicating with your coach!</div>';
            return;
        }

        box.innerHTML = messages.map(m => {
            const isMe = m.sender_id === AppState.currentUser.id;
            return `
                <div class="chat-bubble ${isMe ? 'sent' : 'received'}">
                    <div>${escapeHtml(m.message_text)}</div>
                    <span class="msg-time">${m.sent_at ? m.sent_at.substring(11, 16) : ''}</span>
                </div>
            `;
        }).join('');
        box.scrollTop = box.scrollHeight;
    } catch (e) {}
}

async function sendUserMessage(e) {
    e.preventDefault();
    const input = document.getElementById('user-input-text');
    const text = input.value.trim();
    if (!text || !AppState.activeChatContactId) return;

    try {
        const res = await apiRequest('/api/user/messages', 'POST', {
            senderId: AppState.currentUser.id,
            receiverId: AppState.activeChatContactId,
            messageText: text
        });
        input.value = '';
        showToast(res.message);
        loadUserMessages(AppState.activeChatContactId);
    } catch (err) {
        showToast(err.message, true);
    }
}

async function loadUserHistoryTab(container) {
    try {
        const [logsRes, achRes] = await Promise.all([
            apiRequest(`/api/user/progress/logs?userId=${AppState.currentUser.id}`),
            apiRequest(`/api/user/achievements?userId=${AppState.currentUser.id}`)
        ]);

        const logs = logsRes.data || [];
        const achievements = (achRes.data && achRes.data.badges) || [];

        container.innerHTML = `
            <!-- Gamified Achievements -->
            <div class="panel">
                <div class="panel-header">
                    <div class="panel-title">🏅 Milestones & Achievements</div>
                </div>
                <div class="achievements-grid">
                    ${achievements.map(b => `
                        <div class="achievement-card ${b.earned ? 'earned' : ''}">
                            <div class="achievement-icon">${b.earned ? '🥇' : '🔒'}</div>
                            <div class="achievement-title">${escapeHtml(b.title)}</div>
                            <div class="achievement-desc">${escapeHtml(b.description)}</div>
                            <div style="margin-top:8px;">
                                <span class="badge ${b.earned ? 'badge-approved' : 'badge-inactive'}">${b.earned ? 'UNLOCKED' : 'LOCKED'}</span>
                            </div>
                        </div>
                    `).join('')}
                </div>
            </div>

            <!-- Workout Logs Table -->
            <div class="panel">
                <div class="panel-header">
                    <div class="panel-title">📜 Workout History Log</div>
                    <button class="btn btn-primary btn-sm" onclick="openLogWorkoutModal()">+ Log Session</button>
                </div>
                <div class="table-container">
                    <table>
                        <thead>
                            <tr>
                                <th>Date</th>
                                <th>Workout Routine</th>
                                <th>Duration</th>
                                <th>Calories</th>
                                <th>Recorded Weight</th>
                                <th>Notes / Experience</th>
                                <th>Action</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${logs.length ? logs.map(l => `
                                <tr>
                                    <td><strong>${l.log_date}</strong></td>
                                    <td><span class="badge" style="background:#1e3a8a;color:#93c5fd;">${escapeHtml(l.plan_title)}</span></td>
                                    <td>${l.duration_minutes} mins</td>
                                    <td><strong>${l.calories_burned}</strong> kcal</td>
                                    <td>${l.weight > 0 ? l.weight + ' kg' : '--'}</td>
                                    <td style="max-width:240px;font-size:0.85rem;color:var(--text-secondary);">${escapeHtml(l.notes || 'None')}</td>
                                    <td>
                                        <button class="btn btn-danger btn-sm" onclick="deleteWorkoutLog(${l.id})">Delete</button>
                                    </td>
                                </tr>
                            `).join('') : '<tr><td colspan="7" style="text-align:center;padding:24px;color:var(--text-muted)">No workouts logged yet. Complete a workout and record it!</td></tr>'}
                        </tbody>
                    </table>
                </div>
            </div>
        `;
    } catch (err) {
        container.innerHTML = `<div class="panel" style="color:var(--danger)">Failed to load history: ${err.message}</div>`;
    }
}

async function deleteWorkoutLog(logId) {
    if (!confirm('Are you sure you want to remove this workout log?')) return;
    try {
        const res = await apiRequest(`/api/user/progress/log?id=${logId}&userId=${AppState.currentUser.id}`, 'DELETE');
        showToast(res.message);
        loadUserHistoryTab(document.getElementById('user-tab-content'));
    } catch (err) {
        showToast(err.message, true);
    }
}

async function loadUserProfileTab(container) {
    try {
        const res = await apiRequest(`/api/user/profile?userId=${AppState.currentUser.id}`);
        const user = res.data.user || {};
        const profile = res.data.profile || {};

        container.innerHTML = `
            <div class="panel">
                <div class="panel-header">
                    <div class="panel-title">👤 Profile & Fitness Goals Management</div>
                </div>
                <form id="user-profile-form" onsubmit="saveUserProfile(event)">
                    <div class="form-grid">
                        <div class="form-group">
                            <label class="form-label">Full Name</label>
                            <input type="text" class="form-control" name="name" value="${escapeHtml(user.name || '')}" required>
                        </div>
                        <div class="form-group">
                            <label class="form-label">Email Address (Read Only)</label>
                            <input type="email" class="form-control" value="${escapeHtml(user.email || '')}" disabled style="opacity:0.7;">
                        </div>
                        <div class="form-group">
                            <label class="form-label">Age</label>
                            <input type="number" class="form-control" name="age" value="${profile.age || 25}" min="14" max="100">
                        </div>
                        <div class="form-group">
                            <label class="form-label">Gender</label>
                            <select class="form-control" name="gender">
                                <option value="Male" ${profile.gender === 'Male' ? 'selected' : ''}>Male</option>
                                <option value="Female" ${profile.gender === 'Female' ? 'selected' : ''}>Female</option>
                                <option value="Other" ${profile.gender === 'Other' ? 'selected' : ''}>Other / Prefer not to say</option>
                            </select>
                        </div>
                        <div class="form-group">
                            <label class="form-label">Height (cm)</label>
                            <input type="number" step="0.5" class="form-control" name="height" value="${profile.height || 170.0}">
                        </div>
                        <div class="form-group">
                            <label class="form-label">Current Weight (kg)</label>
                            <input type="number" step="0.1" class="form-control" name="currentWeight" value="${profile.current_weight || 70.0}">
                        </div>
                        <div class="form-group">
                            <label class="form-label">Target Goal Weight (kg)</label>
                            <input type="number" step="0.1" class="form-control" name="targetWeight" value="${profile.target_weight || 68.0}">
                        </div>
                        <div class="form-group">
                            <label class="form-label">Primary Fitness Goal</label>
                            <input type="text" class="form-control" name="fitnessGoal" value="${escapeHtml(profile.fitness_goal || 'General Fitness')}">
                        </div>
                    </div>
                    <div class="form-group">
                        <label class="form-label">About You / Fitness Bio</label>
                        <textarea class="form-control" name="bio">${escapeHtml(user.bio || '')}</textarea>
                    </div>
                    <div class="form-group">
                        <label class="form-label">Update Password (leave blank to keep current)</label>
                        <input type="password" class="form-control" name="password" placeholder="New password...">
                    </div>
                    <button type="submit" class="btn btn-primary" style="margin-top:10px;">Save Profile Changes</button>
                </form>
            </div>
        `;
    } catch (err) {
        container.innerHTML = `<div class="panel" style="color:var(--danger)">Failed to load profile: ${err.message}</div>`;
    }
}

async function saveUserProfile(e) {
    e.preventDefault();
    const form = e.target;
    const formData = new FormData(form);
    const payload = { userId: AppState.currentUser.id };
    formData.forEach((val, key) => payload[key] = val);

    try {
        const res = await apiRequest('/api/user/profile', 'POST', payload);
        showToast(res.message);
        if (payload.name) {
            AppState.currentUser.name = payload.name;
            localStorage.setItem('apexfit_user', JSON.stringify(AppState.currentUser));
            renderRoleBar();
            renderNavbar();
        }
    } catch (err) {
        showToast(err.message, true);
    }
}

// ==========================================
// MODALS MANAGEMENT
// ==========================================
function openModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) modal.classList.add('active');
}

function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) modal.classList.remove('active');
}

// 1. Plan Exercises Modal
async function viewPlanExercises(planId) {
    try {
        const res = await apiRequest(`/api/user/plan/details?id=${planId}`);
        const plan = res.data;
        const exercises = plan.exercises || [];

        const modal = document.getElementById('generic-modal');
        modal.querySelector('.modal-header h3').innerText = `📋 ${plan.title} - Daily Routine`;
        modal.querySelector('.modal-body').innerHTML = `
            <div style="margin-bottom:16px;">
                <p style="color:var(--text-secondary);font-size:0.9rem;margin-bottom:8px;">${escapeHtml(plan.description)}</p>
                <div class="plan-meta">
                    <span>Coach: <strong>${escapeHtml(plan.coach_name)}</strong></span>
                    <span>Category: <strong>${escapeHtml(plan.category)}</strong></span>
                    <span>Difficulty: <strong>${plan.difficulty}</strong></span>
                    <span>Duration: <strong>${plan.duration_weeks} Weeks</strong></span>
                </div>
            </div>
            <div class="table-container">
                <table>
                    <thead>
                        <tr>
                            <th>Day / Session</th>
                            <th>Exercise Name</th>
                            <th>Sets</th>
                            <th>Reps</th>
                            <th>Duration</th>
                            <th>Rest</th>
                            <th>Coaching Notes</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${exercises.length ? exercises.map(ex => `
                            <tr>
                                <td><span class="badge" style="background:#1e3a8a;color:#93c5fd;">${escapeHtml(ex.day_of_week)}</span></td>
                                <td><strong>${escapeHtml(ex.exercise_name)}</strong></td>
                                <td>${ex.sets}</td>
                                <td>${ex.reps}</td>
                                <td>${ex.duration_mins > 0 ? ex.duration_mins + 'm' : '--'}</td>
                                <td>${ex.rest_seconds}s</td>
                                <td style="font-size:0.8rem;color:var(--text-secondary);">${escapeHtml(ex.notes || 'Strict form')}</td>
                            </tr>
                        `).join('') : '<tr><td colspan="7" style="text-align:center;padding:16px;color:var(--text-muted)">No exercises configured for this plan yet.</td></tr>'}
                    </tbody>
                </table>
            </div>
        `;
        modal.querySelector('.modal-footer').innerHTML = `<button class="btn btn-secondary" onclick="closeModal('generic-modal')">Close</button>`;
        openModal('generic-modal');
    } catch (err) {
        showToast(err.message, true);
    }
}

// 2. Trainee Progress Modal (Coach view)
async function viewTraineeProgressModal(traineeId, traineeName) {
    try {
        const res = await apiRequest(`/api/coach/trainee/progress?traineeId=${traineeId}`);
        const data = res.data;
        const totals = data.totals || {};
        const profile = data.profile || {};
        const history = data.history || [];
        const logs = data.logs || [];

        const modal = document.getElementById('generic-modal');
        modal.querySelector('.modal-header h3').innerText = `📊 Progress Report: ${traineeName}`;
        modal.querySelector('.modal-body').innerHTML = `
            <div class="stats-grid" style="margin-bottom:16px;">
                <div class="stat-card" style="padding:12px;">
                    <div class="stat-card-info">
                        <h4>Workouts</h4>
                        <div class="stat-value" style="font-size:1.4rem;">${totals.total_workouts || 0}</div>
                    </div>
                </div>
                <div class="stat-card" style="padding:12px;">
                    <div class="stat-card-info">
                        <h4>Calories</h4>
                        <div class="stat-value" style="font-size:1.4rem;">${totals.total_calories || 0} kcal</div>
                    </div>
                </div>
                <div class="stat-card" style="padding:12px;">
                    <div class="stat-card-info">
                        <h4>Current Weight</h4>
                        <div class="stat-value" style="font-size:1.4rem;">${profile.current_weight || '--'} kg</div>
                    </div>
                </div>
            </div>
            <div style="margin-bottom:16px;">
                <h4 style="font-size:0.9rem;color:var(--text-secondary);margin-bottom:6px;">Weight Progression Trend</h4>
                <div class="chart-box" id="trainee-modal-chart" style="min-height:180px;"></div>
            </div>
            <div class="table-container">
                <table>
                    <thead>
                        <tr>
                            <th>Date</th>
                            <th>Routine</th>
                            <th>Duration</th>
                            <th>Calories</th>
                            <th>Weight</th>
                            <th>Notes</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${logs.slice(0, 5).map(l => `
                            <tr>
                                <td>${l.log_date}</td>
                                <td>${escapeHtml(l.plan_title)}</td>
                                <td>${l.duration_minutes}m</td>
                                <td>${l.calories_burned}</td>
                                <td>${l.weight > 0 ? l.weight + 'kg' : '--'}</td>
                                <td style="font-size:0.8rem;">${escapeHtml(l.notes || '')}</td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;
        modal.querySelector('.modal-footer').innerHTML = `<button class="btn btn-secondary" onclick="closeModal('generic-modal')">Close</button>`;
        openModal('generic-modal');

        if (history.length) {
            const weightPoints = history.filter(h => h.weight > 0).map(h => ({ label: h.log_date.substring(5), value: h.weight }));
            renderLineChart('trainee-modal-chart', weightPoints, '#10b981', 'Weight (kg)');
        }
    } catch (err) {
        showToast(err.message, true);
    }
}

// 3. Moderate Content Modal
function openModerateModal(plan) {
    const modal = document.getElementById('generic-modal');
    modal.querySelector('.modal-header h3').innerText = `🛡️ Moderate Content: ${plan.title}`;
    modal.querySelector('.modal-body').innerHTML = `
        <div style="margin-bottom:16px;">
            <div class="plan-meta">
                <span>Coach: <strong>${escapeHtml(plan.coach_name)}</strong></span>
                <span>Category: <strong>${escapeHtml(plan.category)}</strong></span>
                <span>Difficulty: <strong>${plan.difficulty}</strong></span>
                <span>Current Status: <span class="badge badge-${plan.approval_status.toLowerCase()}">${plan.approval_status}</span></span>
            </div>
            <p style="font-size:0.9rem;color:var(--text-secondary);margin-bottom:16px;">${escapeHtml(plan.description)}</p>
            <div class="form-group">
                <label class="form-label">Administrator Moderation Notes</label>
                <textarea id="mod-notes-input" class="form-control" placeholder="Enter notes or guidance for coach...">${escapeHtml(plan.moderation_notes || '')}</textarea>
            </div>
        </div>
    `;
    modal.querySelector('.modal-footer').innerHTML = `
        <button class="btn btn-danger" onclick="submitModeration(${plan.id}, 'REJECTED')">Reject Plan</button>
        <button class="btn btn-primary" onclick="submitModeration(${plan.id}, 'APPROVED')">Approve Plan</button>
        <button class="btn btn-secondary" onclick="closeModal('generic-modal')">Cancel</button>
    `;
    openModal('generic-modal');
}

async function submitModeration(planId, status) {
    const notes = document.getElementById('mod-notes-input').value.trim();
    try {
        const res = await apiRequest('/api/admin/content/moderate', 'POST', {
            planId,
            status,
            moderationNotes: notes
        });
        showToast(res.message);
        closeModal('generic-modal');
        filterAdminContent();
    } catch (err) {
        showToast(err.message, true);
    }
}

// 4. Create User Modal
function openCreateUserModal() {
    const modal = document.getElementById('generic-modal');
    modal.querySelector('.modal-header h3').innerText = '👥 Add New User Account';
    modal.querySelector('.modal-body').innerHTML = `
        <form id="create-user-form" onsubmit="submitCreateUser(event)">
            <div class="form-group">
                <label class="form-label">Full Name</label>
                <input type="text" class="form-control" name="name" required placeholder="Jane Doe">
            </div>
            <div class="form-group">
                <label class="form-label">Email Address</label>
                <input type="email" class="form-control" name="email" required placeholder="user@fitness.com">
            </div>
            <div class="form-group">
                <label class="form-label">Password</label>
                <input type="password" class="form-control" name="password" required placeholder="Password...">
            </div>
            <div class="form-grid">
                <div class="form-group">
                    <label class="form-label">Role</label>
                    <select class="form-control" name="role">
                        <option value="USER">Trainee / User</option>
                        <option value="COACH">Fitness Coach</option>
                        <option value="ADMIN">System Administrator</option>
                    </select>
                </div>
                <div class="form-group">
                    <label class="form-label">Account Status</label>
                    <select class="form-control" name="status">
                        <option value="ACTIVE">Active</option>
                        <option value="INACTIVE">Inactive</option>
                    </select>
                </div>
            </div>
            <div class="form-group">
                <label class="form-label">Bio / Specialty</label>
                <textarea class="form-control" name="bio" placeholder="Credentials or background..."></textarea>
            </div>
            <div class="modal-footer" style="padding:16px 0 0 0;background:transparent;">
                <button type="button" class="btn btn-secondary" onclick="closeModal('generic-modal')">Cancel</button>
                <button type="submit" class="btn btn-primary">Create User Account</button>
            </div>
        </form>
    `;
    modal.querySelector('.modal-footer').innerHTML = '';
    openModal('generic-modal');
}

async function submitCreateUser(e) {
    e.preventDefault();
    const formData = new FormData(e.target);
    const payload = {};
    formData.forEach((v, k) => payload[k] = v);

    try {
        const res = await apiRequest('/api/admin/users', 'POST', payload);
        showToast(res.message);
        closeModal('generic-modal');
        filterAdminUsers();
    } catch (err) {
        showToast(err.message, true);
    }
}

// 5. Edit User Modal
function openEditUserModal(user) {
    const modal = document.getElementById('generic-modal');
    modal.querySelector('.modal-header h3').innerText = `✏️ Edit User: ${user.name}`;
    modal.querySelector('.modal-body').innerHTML = `
        <form id="edit-user-form" onsubmit="submitEditUser(event, ${user.id})">
            <div class="form-group">
                <label class="form-label">Full Name</label>
                <input type="text" class="form-control" name="name" value="${escapeHtml(user.name)}" required>
            </div>
            <div class="form-group">
                <label class="form-label">Email Address</label>
                <input type="email" class="form-control" name="email" value="${escapeHtml(user.email)}" required>
            </div>
            <div class="form-group">
                <label class="form-label">Reset Password (leave blank to keep unchanged)</label>
                <input type="password" class="form-control" name="password" placeholder="New password...">
            </div>
            <div class="form-grid">
                <div class="form-group">
                    <label class="form-label">Role</label>
                    <select class="form-control" name="role">
                        <option value="USER" ${user.role === 'USER' ? 'selected' : ''}>Trainee / User</option>
                        <option value="COACH" ${user.role === 'COACH' ? 'selected' : ''}>Fitness Coach</option>
                        <option value="ADMIN" ${user.role === 'ADMIN' ? 'selected' : ''}>System Administrator</option>
                    </select>
                </div>
                <div class="form-group">
                    <label class="form-label">Account Status</label>
                    <select class="form-control" name="status">
                        <option value="ACTIVE" ${user.status === 'ACTIVE' ? 'selected' : ''}>Active</option>
                        <option value="INACTIVE" ${user.status === 'INACTIVE' ? 'selected' : ''}>Inactive</option>
                    </select>
                </div>
            </div>
            <div class="form-group">
                <label class="form-label">Bio / Specialty</label>
                <textarea class="form-control" name="bio">${escapeHtml(user.bio || '')}</textarea>
            </div>
            <div class="modal-footer" style="padding:16px 0 0 0;background:transparent;">
                <button type="button" class="btn btn-secondary" onclick="closeModal('generic-modal')">Cancel</button>
                <button type="submit" class="btn btn-primary">Save Changes</button>
            </div>
        </form>
    `;
    modal.querySelector('.modal-footer').innerHTML = '';
    openModal('generic-modal');
}

async function submitEditUser(e, userId) {
    e.preventDefault();
    const formData = new FormData(e.target);
    const payload = { id: userId };
    formData.forEach((v, k) => payload[k] = v);

    try {
        const res = await apiRequest('/api/admin/users', 'PUT', payload);
        showToast(res.message);
        closeModal('generic-modal');
        filterAdminUsers();
    } catch (err) {
        showToast(err.message, true);
    }
}

// 6. Create Workout Plan Modal (Coach)
function openCreatePlanModal() {
    const modal = document.getElementById('generic-modal');
    modal.querySelector('.modal-header h3').innerText = '📋 Create New Workout Plan';
    modal.querySelector('.modal-body').innerHTML = `
        <form id="create-plan-form" onsubmit="submitCreatePlan(event)">
            <div class="form-grid">
                <div class="form-group">
                    <label class="form-label">Plan Title</label>
                    <input type="text" class="form-control" name="title" required placeholder="e.g. 8-Week Hypertrophy Protocol">
                </div>
                <div class="form-group">
                    <label class="form-label">Category</label>
                    <select class="form-control" name="category">
                        <option value="Fat Loss">Fat Loss & Conditioning</option>
                        <option value="Muscle Building">Muscle Building & Hypertrophy</option>
                        <option value="HIIT Cardio">HIIT & Metabolic Conditioning</option>
                        <option value="Yoga & Mobility">Yoga & Mobility</option>
                        <option value="Powerlifting">Powerlifting & Strength</option>
                    </select>
                </div>
                <div class="form-group">
                    <label class="form-label">Difficulty Level</label>
                    <select class="form-control" name="difficulty">
                        <option value="BEGINNER">Beginner</option>
                        <option value="INTERMEDIATE" selected>Intermediate</option>
                        <option value="ADVANCED">Advanced</option>
                    </select>
                </div>
                <div class="form-group">
                    <label class="form-label">Duration (Weeks)</label>
                    <input type="number" class="form-control" name="durationWeeks" value="8" min="1" max="52" required>
                </div>
            </div>
            <div class="form-group">
                <label class="form-label">Plan Description & Objectives</label>
                <textarea class="form-control" name="description" required placeholder="Describe goals, target audience, and training frequency..."></textarea>
            </div>

            <!-- Dynamic Exercise List -->
            <div style="margin-top:20px;margin-bottom:12px;display:flex;justify-content:space-between;align-items:center;">
                <h4 style="font-size:0.95rem;color:#fff;">Exercises Schedule</h4>
                <button type="button" class="btn btn-outline btn-sm" onclick="addExerciseRow()">+ Add Exercise</button>
            </div>
            <div id="exercise-rows-container" style="display:flex;flex-direction:column;gap:10px;max-height:260px;overflow-y:auto;padding-right:4px;">
                <!-- dynamic rows -->
            </div>

            <div class="modal-footer" style="padding:16px 0 0 0;background:transparent;margin-top:16px;">
                <button type="button" class="btn btn-secondary" onclick="closeModal('generic-modal')">Cancel</button>
                <button type="submit" class="btn btn-primary">Submit Plan for Moderation</button>
            </div>
        </form>
    `;
    modal.querySelector('.modal-footer').innerHTML = '';
    openModal('generic-modal');

    // Add 2 initial rows
    addExerciseRow('Day 1', 'Barbell Squats', 4, 10, 15, 90, 'Focus on depth');
    addExerciseRow('Day 2', 'Bench Press', 4, 8, 15, 90, 'Touch sternum with control');
}

function addExerciseRow(day = 'Day 1', name = '', sets = 3, reps = 12, duration = 10, rest = 60, notes = '') {
    const container = document.getElementById('exercise-rows-container');
    if (!container) return;

    const row = document.createElement('div');
    row.className = 'exercise-input-row';
    row.style.cssText = 'display:grid;grid-template-columns:100px 1.5fr 60px 60px 70px 30px;gap:8px;align-items:center;background:#182234;padding:8px 10px;border-radius:6px;';
    row.innerHTML = `
        <input type="text" class="form-control" placeholder="Day" value="${escapeHtml(day)}" style="padding:6px;">
        <input type="text" class="form-control" placeholder="Exercise Name" value="${escapeHtml(name)}" required style="padding:6px;">
        <input type="number" class="form-control" placeholder="Sets" value="${sets}" min="1" max="20" style="padding:6px;">
        <input type="number" class="form-control" placeholder="Reps" value="${reps}" min="1" max="100" style="padding:6px;">
        <input type="number" class="form-control" placeholder="Rest(s)" value="${rest}" min="0" max="600" style="padding:6px;">
        <button type="button" style="background:transparent;border:none;color:var(--danger);cursor:pointer;font-size:1.1rem;" onclick="this.parentElement.remove()">✕</button>
    `;
    container.appendChild(row);
}

async function submitCreatePlan(e) {
    e.preventDefault();
    const form = e.target;
    const title = form.title.value.trim();
    const category = form.category.value;
    const difficulty = form.difficulty.value;
    const durationWeeks = parseInt(form.durationWeeks.value);
    const description = form.description.value.trim();

    const exerciseRows = document.querySelectorAll('#exercise-rows-container .exercise-input-row');
    const exercises = [];
    exerciseRows.forEach(row => {
        const inputs = row.querySelectorAll('input');
        exercises.push({
            dayOfWeek: inputs[0].value.trim() || 'Day 1',
            exerciseName: inputs[1].value.trim(),
            sets: parseInt(inputs[2].value) || 3,
            reps: parseInt(inputs[3].value) || 12,
            durationMins: 10,
            restSeconds: parseInt(inputs[4].value) || 60,
            notes: ''
        });
    });

    try {
        const res = await apiRequest('/api/coach/plans', 'POST', {
            coachId: AppState.currentUser.id,
            title,
            category,
            difficulty,
            durationWeeks,
            description,
            exercises
        });
        showToast(res.message);
        closeModal('generic-modal');
        loadCoachPlansTab(document.getElementById('coach-tab-content'));
    } catch (err) {
        showToast(err.message, true);
    }
}

// 7. Log Workout Modal (User)
async function openLogWorkoutModal(preSelectedPlanId = null, preSelectedPlanTitle = '') {
    // Fetch active enrolled plans for dropdown
    let activePlans = [];
    try {
        const res = await apiRequest(`/api/user/plans/active?userId=${AppState.currentUser.id}`);
        activePlans = res.data || [];
    } catch (e) {}

    const today = new Date().toISOString().substring(0, 10);
    const modal = document.getElementById('generic-modal');
    modal.querySelector('.modal-header h3').innerText = '🔥 Log Workout Session';
    modal.querySelector('.modal-body').innerHTML = `
        <form id="log-workout-form" onsubmit="submitLogWorkout(event)">
            <div class="form-grid">
                <div class="form-group">
                    <label class="form-label">Workout Date</label>
                    <input type="date" class="form-control" name="logDate" value="${today}" required>
                </div>
                <div class="form-group">
                    <label class="form-label">Associated Workout Plan</label>
                    <select class="form-control" name="planId">
                        <option value="">-- General / Custom Session --</option>
                        ${activePlans.map(p => `
                            <option value="${p.id}" ${preSelectedPlanId === p.id ? 'selected' : ''}>${escapeHtml(p.title)}</option>
                        `).join('')}
                    </select>
                </div>
                <div class="form-group">
                    <label class="form-label">Duration (Minutes)</label>
                    <input type="number" class="form-control" name="durationMinutes" value="45" min="5" max="300" required>
                </div>
                <div class="form-group">
                    <label class="form-label">Calories Burned (kcal)</label>
                    <input type="number" class="form-control" name="caloriesBurned" value="420" min="10" max="3000" required>
                </div>
                <div class="form-group">
                    <label class="form-label">Current Body Weight (kg)</label>
                    <input type="number" step="0.1" class="form-control" name="weight" value="83.5" min="30" max="250">
                </div>
            </div>
            <div class="form-group">
                <label class="form-label">Session Notes / RPE / Progression</label>
                <textarea class="form-control" name="notes" placeholder="e.g. Squats felt great! Increased working weight by 2.5kg."></textarea>
            </div>
            <div class="modal-footer" style="padding:16px 0 0 0;background:transparent;">
                <button type="button" class="btn btn-secondary" onclick="closeModal('generic-modal')">Cancel</button>
                <button type="submit" class="btn btn-primary">Save Workout Session</button>
            </div>
        </form>
    `;
    modal.querySelector('.modal-footer').innerHTML = '';
    openModal('generic-modal');
}

async function submitLogWorkout(e) {
    e.preventDefault();
    const formData = new FormData(e.target);
    const payload = { userId: AppState.currentUser.id };
    formData.forEach((v, k) => payload[k] = v);

    try {
        const res = await apiRequest('/api/user/progress/log', 'POST', payload);
        showToast(res.message);
        closeModal('generic-modal');
        if (AppState.activeTab === 'tracker') {
            loadUserTrackerTab(document.getElementById('user-tab-content'));
        } else if (AppState.activeTab === 'history') {
            loadUserHistoryTab(document.getElementById('user-tab-content'));
        }
    } catch (err) {
        showToast(err.message, true);
    }
}

// 8. Submit Feedback Modal
function openFeedbackModal() {
    const modal = document.getElementById('generic-modal');
    modal.querySelector('.modal-header h3').innerText = '💬 Submit Trainee Feedback';
    modal.querySelector('.modal-body').innerHTML = `
        <form id="feedback-form" onsubmit="submitFeedback(event)">
            <div class="form-grid">
                <div class="form-group">
                    <label class="form-label">Category</label>
                    <select class="form-control" name="category">
                        <option value="Workout Plan">Workout Plan Quality</option>
                        <option value="Coaching Interaction">Coaching Interaction</option>
                        <option value="Platform Usability">Platform Usability</option>
                        <option value="Feature Request">Feature Request</option>
                    </select>
                </div>
                <div class="form-group">
                    <label class="form-label">Star Rating (1 - 5)</label>
                    <select class="form-control" name="rating">
                        <option value="5" selected>★★★★★ (5 - Excellent)</option>
                        <option value="4">★★★★☆ (4 - Good)</option>
                        <option value="3">★★★☆☆ (3 - Average)</option>
                        <option value="2">★★☆☆☆ (2 - Below Average)</option>
                        <option value="1">★☆☆☆☆ (1 - Poor)</option>
                    </select>
                </div>
            </div>
            <div class="form-group">
                <label class="form-label">Subject</label>
                <input type="text" class="form-control" name="subject" required placeholder="Brief summary of your feedback...">
            </div>
            <div class="form-group">
                <label class="form-label">Your Feedback / Review Message</label>
                <textarea class="form-control" name="message" required placeholder="Share your experience or suggestions for the platform administrators..."></textarea>
            </div>
            <div class="modal-footer" style="padding:16px 0 0 0;background:transparent;">
                <button type="button" class="btn btn-secondary" onclick="closeModal('generic-modal')">Cancel</button>
                <button type="submit" class="btn btn-primary">Submit Feedback</button>
            </div>
        </form>
    `;
    modal.querySelector('.modal-footer').innerHTML = '';
    openModal('generic-modal');
}

async function submitFeedback(e) {
    e.preventDefault();
    const formData = new FormData(e.target);
    const payload = { userId: AppState.currentUser.id };
    formData.forEach((v, k) => payload[k] = v);

    try {
        const res = await apiRequest('/api/user/feedback', 'POST', payload);
        showToast(res.message);
        closeModal('generic-modal');
    } catch (err) {
        showToast(err.message, true);
    }
}

// 9. Login & Registration Modal
function showLoginModal() {
    const modal = document.getElementById('generic-modal');
    modal.querySelector('.modal-header h3').innerText = '🔐 Platform Login & Registration';
    modal.querySelector('.modal-body').innerHTML = `
        <div style="display:flex;gap:12px;margin-bottom:18px;">
            <button class="btn btn-primary" id="btn-show-login" style="flex:1;" onclick="toggleAuthForm('login')">Sign In</button>
            <button class="btn btn-secondary" id="btn-show-register" style="flex:1;" onclick="toggleAuthForm('register')">Register Trainee</button>
        </div>
        <div id="auth-form-container">
            <form id="login-form" onsubmit="handleAuthLogin(event)">
                <div class="form-group">
                    <label class="form-label">Email Address</label>
                    <input type="email" class="form-control" name="email" required placeholder="admin@fitness.com">
                </div>
                <div class="form-group">
                    <label class="form-label">Password</label>
                    <input type="password" class="form-control" name="password" required placeholder="••••••••">
                </div>
                <button type="submit" class="btn btn-primary" style="width:100%;margin-top:10px;">Sign In to Account</button>
            </form>
        </div>
    `;
    modal.querySelector('.modal-footer').innerHTML = '';
    openModal('generic-modal');
}

function toggleAuthForm(mode) {
    const container = document.getElementById('auth-form-container');
    const btnLogin = document.getElementById('btn-show-login');
    const btnReg = document.getElementById('btn-show-register');

    if (mode === 'login') {
        btnLogin.className = 'btn btn-primary';
        btnReg.className = 'btn btn-secondary';
        container.innerHTML = `
            <form id="login-form" onsubmit="handleAuthLogin(event)">
                <div class="form-group">
                    <label class="form-label">Email Address</label>
                    <input type="email" class="form-control" name="email" required placeholder="admin@fitness.com">
                </div>
                <div class="form-group">
                    <label class="form-label">Password</label>
                    <input type="password" class="form-control" name="password" required placeholder="••••••••">
                </div>
                <button type="submit" class="btn btn-primary" style="width:100%;margin-top:10px;">Sign In to Account</button>
            </form>
        `;
    } else {
        btnLogin.className = 'btn btn-secondary';
        btnReg.className = 'btn btn-primary';
        container.innerHTML = `
            <form id="register-form" onsubmit="handleAuthRegister(event)">
                <div class="form-group">
                    <label class="form-label">Full Name</label>
                    <input type="text" class="form-control" name="name" required placeholder="Alex Mercer">
                </div>
                <div class="form-group">
                    <label class="form-label">Email Address</label>
                    <input type="email" class="form-control" name="email" required placeholder="alex@gmail.com">
                </div>
                <div class="form-group">
                    <label class="form-label">Password</label>
                    <input type="password" class="form-control" name="password" required placeholder="Create strong password">
                </div>
                <div class="form-group">
                    <label class="form-label">Role</label>
                    <select class="form-control" name="role">
                        <option value="USER" selected>Trainee / Fitness Enthusiast</option>
                        <option value="COACH">Fitness Coach</option>
                    </select>
                </div>
                <button type="submit" class="btn btn-primary" style="width:100%;margin-top:10px;">Create Account</button>
            </form>
        `;
    }
}

async function handleAuthLogin(e) {
    e.preventDefault();
    const form = e.target;
    await quickLogin(form.email.value.trim(), form.password.value.trim());
    closeModal('generic-modal');
}

async function handleAuthRegister(e) {
    e.preventDefault();
    const form = e.target;
    const name = form.name.value.trim();
    const email = form.email.value.trim();
    const password = form.password.value.trim();
    const role = form.role.value;

    try {
        const res = await apiRequest('/api/auth/register', 'POST', { name, email, password, role });
        showToast(res.message);
        AppState.currentUser = res.data;
        localStorage.setItem('apexfit_user', JSON.stringify(AppState.currentUser));
        closeModal('generic-modal');
        setDefaultTabForRole(AppState.currentUser.role);
        renderApp();
    } catch (err) {
        showToast(err.message, true);
    }
}

// ==========================================
// OFFLINE SVG CHARTS RENDERING ENGINE
// ==========================================
function renderDonutChart(containerId, slices) {
    const el = document.getElementById(containerId);
    if (!el) return;

    const total = slices.reduce((acc, s) => acc + s.value, 0);
    if (total === 0) {
        el.innerHTML = '<div style="color:var(--text-muted);">No data available</div>';
        return;
    }

    const size = 180;
    const strokeWidth = 24;
    const radius = (size - strokeWidth) / 2;
    const circumference = 2 * Math.PI * radius;

    let offset = 0;
    let circlesHtml = '';

    slices.forEach(slice => {
        const strokeDash = (slice.value / total) * circumference;
        circlesHtml += `
            <circle cx="${size/2}" cy="${size/2}" r="${radius}" fill="transparent"
                stroke="${slice.color}" stroke-width="${strokeWidth}"
                stroke-dasharray="${strokeDash} ${circumference}"
                stroke-dashoffset="${-offset}"
                style="transition: stroke-dasharray 0.5s ease;"
            />
        `;
        offset += strokeDash;
    });

    const legendHtml = slices.map(s => `
        <div style="display:flex;align-items:center;gap:6px;font-size:0.8rem;">
            <span style="display:inline-block;width:10px;height:10px;border-radius:2px;background:${s.color};"></span>
            <span style="color:var(--text-secondary);">${escapeHtml(s.label)}:</span>
            <strong>${s.value}</strong>
        </div>
    `).join('');

    el.innerHTML = `
        <div style="display:flex;align-items:center;justify-content:center;gap:24px;width:100%;">
            <svg width="${size}" height="${size}" viewBox="0 0 ${size} ${size}" style="transform: rotate(-90deg);overflow:visible;">
                ${circlesHtml}
                <text x="${size/2}" y="${size/2}" text-anchor="middle" dominant-baseline="middle"
                    fill="#fff" font-size="1.4rem" font-weight="700" style="transform: rotate(90deg);transform-origin: center;">
                    ${total}
                </text>
            </svg>
            <div style="display:flex;flex-direction:column;gap:8px;">${legendHtml}</div>
        </div>
    `;
}

function renderBarChart(containerId, bars) {
    const el = document.getElementById(containerId);
    if (!el) return;

    if (!bars || !bars.length) {
        el.innerHTML = '<div style="color:var(--text-muted);">No data available</div>';
        return;
    }

    const maxVal = Math.max(...bars.map(b => b.value), 1);
    const chartHeight = 160;

    const barsHtml = bars.map(b => {
        const heightPct = Math.round((b.value / maxVal) * 100);
        return `
            <div style="flex:1;display:flex;flex-direction:column;align-items:center;gap:6px;height:100%;justify-content:flex-end;">
                <div style="font-size:0.75rem;font-weight:700;color:#fff;">${b.value}</div>
                <div style="width:100%;max-width:36px;height:${Math.max(heightPct, 4)}%;background:${b.color || 'var(--primary)'};border-radius:4px 4px 0 0;transition: height 0.4s ease;"></div>
                <div style="font-size:0.7rem;color:var(--text-secondary);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;max-width:50px;">${escapeHtml(b.label)}</div>
            </div>
        `;
    }).join('');

    el.innerHTML = `
        <div style="width:100%;height:${chartHeight}px;display:flex;align-items:flex-end;gap:12px;padding:0 8px;">
            ${barsHtml}
        </div>
    `;
}

function renderLineChart(containerId, points, strokeColor = '#10b981', label = 'Value') {
    const el = document.getElementById(containerId);
    if (!el) return;

    if (!points || points.length < 2) {
        el.innerHTML = '<div style="color:var(--text-muted);">At least 2 entries required to render trendline.</div>';
        return;
    }

    const width = 450;
    const height = 160;
    const padding = 25;

    const values = points.map(p => p.value);
    const minVal = Math.min(...values);
    const maxVal = Math.max(...values);
    const range = (maxVal - minVal) === 0 ? 1 : (maxVal - minVal);

    const stepX = (width - 2 * padding) / (points.length - 1);
    const coords = points.map((p, i) => {
        const x = padding + i * stepX;
        const y = height - padding - ((p.value - minVal) / range) * (height - 2 * padding);
        return { x, y, val: p.value, label: p.label };
    });

    const pathData = coords.reduce((acc, pt, i) => i === 0 ? `M ${pt.x},${pt.y}` : `${acc} L ${pt.x},${pt.y}`, '');
    const areaData = `${pathData} L ${coords[coords.length - 1].x},${height - padding} L ${coords[0].x},${height - padding} Z`;

    const dotsHtml = coords.map(pt => `
        <circle cx="${pt.x}" cy="${pt.y}" r="4" fill="${strokeColor}" stroke="#1e293b" stroke-width="2">
            <title>${pt.label}: ${pt.val}</title>
        </circle>
        <text x="${pt.x}" y="${height - 5}" font-size="0.65rem" fill="#94a3b8" text-anchor="middle">${escapeHtml(pt.label)}</text>
    `).join('');

    el.innerHTML = `
        <svg width="100%" height="${height}" viewBox="0 0 ${width} ${height}" style="overflow:visible;">
            <defs>
                <linearGradient id="grad-${containerId}" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stop-color="${strokeColor}" stop-opacity="0.3" />
                    <stop offset="100%" stop-color="${strokeColor}" stop-opacity="0.0" />
                </linearGradient>
            </defs>
            <path d="${areaData}" fill="url(#grad-${containerId})" />
            <path d="${pathData}" fill="none" stroke="${strokeColor}" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" />
            ${dotsHtml}
        </svg>
    `;
}

// Utility: escape HTML
function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}
