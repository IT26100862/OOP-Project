/**
 * EstateX – Real Estate Property Listings Portal
 * Core Client-Side Layout & UI Interactive Behaviors
 */

// Global API Base URL
window.API_BASE_URL = window.location.port === '8081' ? '' : 'http://localhost:8081';

document.addEventListener('DOMContentLoaded', () => {
  initModals();
  initDropdowns();
  initToast();
  initAuth();
  initMobileNav();
  initDashboardSidebar();
});

// Modal System
function openModal(modalId) {
  const modal = document.getElementById(modalId);
  if (modal) {
    modal.classList.add('active');
    document.body.style.overflow = 'hidden';
  }
}

function closeModal(modalId) {
  const modal = document.getElementById(modalId);
  if (modal) {
    modal.classList.remove('active');
    document.body.style.overflow = '';
  }
}

function initModals() {
  // Close modals on overlay backdrop click
  document.querySelectorAll('.modal-overlay').forEach(overlay => {
    overlay.addEventListener('click', (e) => {
      if (e.target === overlay) {
        overlay.classList.remove('active');
        document.body.style.overflow = '';
      }
    });
  });

  // Close modals on Escape key
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
      document.querySelectorAll('.modal-overlay.active').forEach(modal => {
        modal.classList.remove('active');
      });
      document.body.style.overflow = '';
    }
  });

  // Attach data-modal-close listeners
  document.querySelectorAll('[data-modal-close]').forEach(btn => {
    btn.addEventListener('click', () => {
      const modalId = btn.getAttribute('data-modal-close');
      if (modalId) {
        closeModal(modalId);
      } else {
        const parentModal = btn.closest('.modal-overlay');
        if (parentModal) parentModal.classList.remove('active');
        document.body.style.overflow = '';
      }
    });
  });

  // Attach data-modal-target listeners
  document.querySelectorAll('[data-modal-target]').forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.preventDefault();
      const targetId = btn.getAttribute('data-modal-target');
      if (targetId) openModal(targetId);
    });
  });
}

// Dropdowns
function initDropdowns() {
  document.addEventListener('click', (e) => {
    const isDropdownButton = e.target.closest('.dropdown-trigger');
    const currentDropdown = isDropdownButton ? isDropdownButton.nextElementSibling : null;

    document.querySelectorAll('.dropdown-menu.active').forEach(menu => {
      if (menu !== currentDropdown) {
        menu.classList.remove('active');
      }
    });

    if (currentDropdown) {
      currentDropdown.classList.toggle('active');
    }
  });
}

// Simple Toast Notification for UI feedback
function showToast(message, type = 'info') {
  let toastContainer = document.getElementById('toast-container');
  if (!toastContainer) {
    toastContainer = document.createElement('div');
    toastContainer.id = 'toast-container';
    toastContainer.style.position = 'fixed';
    toastContainer.style.bottom = '24px';
    toastContainer.style.right = '24px';
    toastContainer.style.zIndex = '9999';
    toastContainer.style.display = 'flex';
    toastContainer.style.flexDirection = 'column';
    toastContainer.style.gap = '8px';
    document.body.appendChild(toastContainer);
  }

  const toast = document.createElement('div');
  toast.style.background = '#0E1627';
  toast.style.color = '#FFEDD8';
  toast.style.padding = '12px 20px';
  toast.style.borderRadius = '8px';
  toast.style.fontSize = '0.9rem';
  toast.style.fontWeight = '500';
  toast.style.boxShadow = '0 6px 18px rgba(14,22,39,0.2)';
  toast.style.borderLeft = type === 'success' ? '4px solid #10B981' : type === 'danger' ? '4px solid #EF4444' : '4px solid #CFAB8D';
  toast.textContent = message;

  toastContainer.appendChild(toast);
  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transition = 'opacity 0.3s ease';
    setTimeout(() => toast.remove(), 300);
  }, 3500);
}

function initToast() {
  // Demo action buttons can trigger prototype notifications
  document.querySelectorAll('[data-demo-toast]').forEach(btn => {
    btn.addEventListener('click', (e) => {
      const msg = btn.getAttribute('data-demo-toast');
      const type = btn.getAttribute('data-demo-type') || 'info';
      showToast(msg, type);
    });
  });
}

// Authentication & Role-Based Navigation Sync
function getAuthUser() {
  try {
    const raw = sessionStorage.getItem('estatex_auth_user') || localStorage.getItem('estatex_auth_user');
    return raw ? JSON.parse(raw) : null;
  } catch (e) {
    return null;
  }
}

function requireAuth(redirectUrl, reason = 'action_required') {
  const user = getAuthUser();
  if (!user) {
    const isPublic = window.location.pathname.includes('/public/');
    const isRoot = !isPublic && !window.location.pathname.includes('/user/') && !window.location.pathname.includes('/agent/') && !window.location.pathname.includes('/admin/');
    const loginPath = isPublic ? 'login.html' : isRoot ? 'public/login.html' : '../public/login.html';
    const target = redirectUrl || (window.location.pathname + window.location.search);
    try {
      sessionStorage.setItem('estatex_redirect_url', target);
    } catch (e) {}
    window.location.href = `${loginPath}?redirect=${encodeURIComponent(target)}&reason=${encodeURIComponent(reason)}`;
    return false;
  }
  return true;
}

function logoutEstateX() {
  try {
    sessionStorage.removeItem('estatex_auth_user');
    localStorage.removeItem('estatex_auth_user');
    sessionStorage.removeItem('estatex_jwt_token');
    sessionStorage.removeItem('estatex_redirect_url');
  } catch (e) {}
  showToast('You have been signed out successfully.', 'info');
  const path = window.location.pathname;
  const isSub = path.includes('/public/') || path.includes('/user/') || path.includes('/agent/') || path.includes('/admin/');
  const target = isSub ? '../public/login.html' : 'public/login.html';
  setTimeout(() => {
    window.location.href = target;
  }, 400);
}

async function deleteAccount() {
  const user = getAuthUser();
  if (!user) {
    showToast('No active user session found.', 'warning');
    return;
  }

  const modal = document.getElementById('deleteAccountModal');
  const btn = modal ? modal.querySelector('.btn-danger') : null;
  const originalText = btn ? btn.innerHTML : 'Delete';
  if (btn) {
    btn.disabled = true;
    btn.innerHTML = 'Deleting...';
  }

  const API_BASE_URL = window.location.port === '8081' ? '' : 'http://localhost:8081';
  const identifier = user.id ? user.id : user.username;

  try {
    const response = await fetch(`${API_BASE_URL}/api/users/${encodeURIComponent(identifier)}`, {
      method: 'DELETE'
    });

    if (response.ok) {
      showToast('Account permanently deleted from system.', 'danger');
      if (modal) closeModal('deleteAccountModal');
      setTimeout(() => {
        logoutEstateX();
      }, 1200);
      return;
    } else {
      const errData = await response.json().catch(() => ({}));
      showToast(errData.message || 'Failed to delete account from server.', 'danger');
      if (btn) {
        btn.disabled = false;
        btn.innerHTML = originalText;
      }
    }
  } catch (err) {
    console.warn('Backend unreachable, simulating offline deletion', err);
    showToast('Account deleted (offline mode).', 'danger');
    if (modal) closeModal('deleteAccountModal');
    setTimeout(() => {
      logoutEstateX();
    }, 1200);
  }
}

async function handleChangePassword(event) {
  event.preventDefault();
  const user = getAuthUser();
  if (!user) return;

  const currentPassword = document.getElementById('currentPassword').value;
  const newPassword = document.getElementById('newPassword').value;
  const confirmPassword = document.getElementById('confirmPassword').value;

  if (newPassword !== confirmPassword) {
    showToast('New passwords do not match!', 'danger');
    return;
  }

  const submitBtn = event.target.querySelector('button[type="submit"]');
  const originalText = submitBtn.innerHTML;
  submitBtn.disabled = true;
  submitBtn.innerHTML = 'Updating...';

  try {
    const API_BASE_URL = window.location.port === '8081' ? '' : 'http://localhost:8081';
    const response = await fetch(`${API_BASE_URL}/api/users/${user.username}/password`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ currentPassword, newPassword })
    });

    if (response.ok) {
      showToast('Password changed successfully! Please log in again.', 'success');
      setTimeout(() => {
        logoutEstateX();
      }, 2000);
    } else {
      const errorData = await response.json().catch(() => ({}));
      showToast(errorData.message || 'Failed to change password.', 'danger');
      submitBtn.disabled = false;
      submitBtn.innerHTML = originalText;
    }
  } catch (err) {
    try {
      let storedUsers = JSON.parse(localStorage.getItem('estatex_registered_users') || '[]');
      const userIndex = storedUsers.findIndex(u => u.username === user.username);
      if (userIndex !== -1) {
        storedUsers[userIndex].password = newPassword;
        localStorage.setItem('estatex_registered_users', JSON.stringify(storedUsers));
      }
    } catch(e) {}

    showToast('Simulated password change successful.', 'success');
    setTimeout(() => {
      logoutEstateX();
    }, 2000);
  }
}

function getProfilePhotoUrl(user) {
  if (user && (user.profilePhotoUrl || user.profileImage)) {
    return user.profilePhotoUrl || user.profileImage;
  }
  return "data:image/svg+xml;base64,PHN2ZyB4bWxucz0naHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmcnIHZpZXdCb3g9JzAgMCAyNCAyNCcgZmlsbD0nI2NiZDVlMSc+PHBhdGggZD0nTTEyIDEyYzIuMjEgMCA0LTEuNzkgNC00cy0xLjc5LTQtNC00LTQgMS43OS00IDQgMS43OSA0IDQgNHptMCAyYy0yLjY3IDAtOCAxLjM0LTggNHYyaDE2di0yYzAtMi42Ni01LjMzLTQtOC00eicvPjwvc3ZnPg==";
}

function handlePhotoUpload(event) {
  const file = event.target.files[0];
  if (!file) return;

  // Validate photo size (max 2MB)
  if (file.size > 2 * 1024 * 1024) {
    showToast('Photo is too large. Please select an image under 2MB.', 'danger');
    event.target.value = '';
    return;
  }

  const reader = new FileReader();
  reader.onload = async function(e) {
    const dataUrl = e.target.result;
    
    // Update active user session immediately
    const user = getAuthUser();
    if (user) {
      user.profilePhotoUrl = dataUrl;
      user.profileImage = dataUrl;
      sessionStorage.setItem('estatex_auth_user', JSON.stringify(user));

      // Update UI avatar elements immediately
      document.querySelectorAll('.user-avatar-circle img, .profile-dropdown-header img, #profilePreviewImage').forEach(img => {
        img.src = dataUrl;
      });

      // Persist to database via REST endpoint
      try {
        const API_BASE_URL = window.location.port === '8081' ? '' : 'http://localhost:8081';
        const response = await fetch(`${API_BASE_URL}/api/users/${encodeURIComponent(user.username)}/photo`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ profileImage: dataUrl })
        });

        if (response.ok) {
          showToast('Profile photo saved to database successfully!', 'success');
        } else {
          showToast('Profile photo updated in session (could not commit to DB).', 'warning');
        }
      } catch (err) {
        console.warn('Backend unavailable, profile photo stored in session.', err);
        showToast('Profile photo updated locally!', 'info');
      }
    } else {
      // If no session user, update preview image anyway
      document.querySelectorAll('.user-avatar-circle img, #profilePreviewImage').forEach(img => {
        img.src = dataUrl;
      });
      showToast('Profile photo updated in preview.', 'info');
    }
  };
  reader.readAsDataURL(file);
}

function initAuth() {
  const path = window.location.pathname;
  // reset-password.html is a public/hybrid utility that manages:
  // 1. Password reset via email token (?token=...)
  // 2. Requesting a reset link (?mode=forgot or unauthenticated access)
  // 3. Authenticated user changing their password from inside the dashboard
  // Therefore, it must NEVER be blocked by the authentication wall.
  const isResetPasswordPage = path.includes('reset-password.html');
  const isProtectedPortal = (path.includes('/user/') || path.includes('/agent/') || path.includes('/admin/')) && !isResetPasswordPage;
  const user = getAuthUser();

  // Enforce authentication on all protected portal routes (excluding reset-password.html)
  if (isProtectedPortal && !user) {
    const redirectTarget = window.location.pathname + window.location.search;
    try {
      sessionStorage.setItem('estatex_redirect_url', redirectTarget);
    } catch (e) {}
    window.location.replace(`../public/login.html?redirect=${encodeURIComponent(redirectTarget)}&reason=auth_required`);
    return;
  }

  // Attach signout handler to all sidebar and footer logout links
  document.querySelectorAll('a[href*="login.html"]').forEach(link => {
    if (link.textContent.toLowerCase().includes('logout') || link.classList.contains('sidebar-item-logout-user')) {
      link.addEventListener('click', (e) => {
        e.preventDefault();
        logoutEstateX();
      });
    }
  });

  const isSub = path.includes('/public/') || path.includes('/user/') || path.includes('/agent/') || path.includes('/admin/');
  const prefix = isSub ? '../' : '';

  // If user is logged in, update public navigation header & dropdown menu
  if (user) {
    const role = (user.role || '').toLowerCase();
    let dashboardPath = prefix + 'user/dashboard.html';
    let profilePath = prefix + 'user/profile.html';
    let roleBadge = '👤 Customer';

    if (role.includes('admin')) {
      dashboardPath = prefix + 'admin/dashboard.html';
      profilePath = prefix + 'admin/profile.html';
      roleBadge = '⚙️ Administrator';
    } else if (role.includes('agent')) {
      dashboardPath = prefix + 'agent/dashboard.html';
      profilePath = prefix + 'agent/profile.html';
      roleBadge = '🏢 Real Estate Agent';
    }

    // Update public profile dropdown
    document.querySelectorAll('.profile-dropdown-menu').forEach(menu => {
      menu.innerHTML = `
        <div class="profile-dropdown-header">
          <div class="user-name">${user.name || 'User'}</div>
          <div class="user-role">${roleBadge}</div>
        </div>
        <a href="${dashboardPath}">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect><rect x="14" y="14" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect></svg>
          My Dashboard
        </a>
        <a href="${profilePath}">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>
          Profile Settings
        </a>
        <div class="profile-dropdown-divider" style="margin: 6px 0; border-top: 1px solid var(--border-light);"></div>
        <a href="javascript:void(0)" onclick="logoutEstateX()" style="color: #EF4444;">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line></svg>
          Sign Out
        </a>
      `;
    });

    // Update public nav actions buttons
    document.querySelectorAll('.public-navbar .nav-actions').forEach(actions => {
      const loginLink = actions.querySelector('a[href*="login.html"]');
      const registerLink = actions.querySelector('a[href*="register.html"]');
      if (loginLink && registerLink) {
        loginLink.href = dashboardPath;
        loginLink.textContent = 'Dashboard';
        registerLink.textContent = 'Sign Out';
        registerLink.removeAttribute('href');
        registerLink.style.cursor = 'pointer';
        registerLink.onclick = logoutEstateX;
      }
    });

    // Update sidebar / header user info on dashboard pages if element exists
    document.querySelectorAll('.user-info-name').forEach(el => {
      el.textContent = user.name;
    });

    // Dynamically update dashboard topbar titles and meta names
    document.querySelectorAll('.user-meta-name').forEach(el => {
      el.textContent = user.name || user.username;
    });

    document.querySelectorAll('.topbar-title').forEach(el => {
      if (el.textContent.includes('Welcome back')) {
        const firstName = (user.name || user.username).split(' ')[0];
        el.textContent = `Welcome back, ${firstName}!`;
      }
    });

    document.querySelectorAll('.user-greeting-name').forEach(el => {
      const firstName = (user.name || user.username).split(' ')[0];
      el.textContent = firstName;
    });

    const footerId = document.getElementById('footerUserId');
    if (footerId && user.id) {
      footerId.textContent = `US-${user.id}`;
    }

    // Dynamically update profile photos globally
    document.querySelectorAll('.user-avatar-circle img, .profile-dropdown-header img, #profilePreviewImage').forEach(img => {
      img.src = getProfilePhotoUrl(user);
    });
  }
}

// ==========================================================================
// MOBILE RESPONSIVE INTERACTIONS
// ==========================================================================

/**
 * Mobile Public Navigation Menu Toggle
 * Handles hamburger button, slide-down drawer, and touch/click interactions
 */
function initMobileNav() {
  const navbars = document.querySelectorAll('.public-navbar');
  navbars.forEach(navbar => {
    const navContainer = navbar.querySelector('.nav-container');
    const navLinks = navbar.querySelector('.nav-links');
    if (!navContainer || !navLinks) return;

    // Ensure toggle button exists in navbar
    let toggleBtn = navContainer.querySelector('.navbar-toggle-btn');
    if (!toggleBtn) {
      toggleBtn = document.createElement('button');
      toggleBtn.className = 'navbar-toggle-btn';
      toggleBtn.setAttribute('aria-label', 'Toggle Navigation Menu');
      toggleBtn.innerHTML = `
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="3" y1="12" x2="21" y2="12"></line>
          <line x1="3" y1="6" x2="21" y2="6"></line>
          <line x1="3" y1="18" x2="21" y2="18"></line>
        </svg>
      `;
      const navActions = navContainer.querySelector('.nav-actions');
      if (navActions) {
        navActions.appendChild(toggleBtn);
      } else {
        navContainer.appendChild(toggleBtn);
      }
    }

    // Ensure mobile actions container exists inside navLinks for responsive auth buttons
    if (!navLinks.querySelector('.mobile-nav-actions')) {
      const mobileActions = document.createElement('div');
      mobileActions.className = 'mobile-nav-actions';
      const user = getAuthUser();
      const isPublic = window.location.pathname.includes('/public/');
      const prefix = isPublic ? '' : 'public/';
      const dashboardPrefix = isPublic ? '../user/dashboard.html' : 'user/dashboard.html';

      if (user) {
        const dashUrl = (user.role || '').toUpperCase() === 'ADMIN' ? (isPublic ? '../admin/properties.html' : 'admin/properties.html')
                      : (user.role || '').toUpperCase() === 'AGENT' ? (isPublic ? '../agent/dashboard.html' : 'agent/dashboard.html')
                      : dashboardPrefix;
        mobileActions.innerHTML = `
          <a href="${dashUrl}" class="btn btn-primary" style="text-align: center;">My Dashboard</a>
          <button type="button" class="btn btn-outline" style="color: #EF4444; border-color: rgba(239, 68, 68, 0.4);" onclick="logoutEstateX()">Sign Out</button>
        `;
      } else {
        mobileActions.innerHTML = `
          <a href="${prefix}login.html" class="btn btn-outline" style="color: var(--primary-light); border-color: rgba(255,237,216,0.3); text-align: center;">Sign In</a>
          <a href="${prefix}register.html" class="btn btn-primary" style="text-align: center;">Create Account</a>
        `;
      }
      navLinks.appendChild(mobileActions);
    }

    // Toggle menu click handler
    toggleBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      const isOpen = navLinks.classList.toggle('mobile-open');
      toggleBtn.setAttribute('aria-expanded', isOpen);
      toggleBtn.innerHTML = isOpen ? `
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="18" y1="6" x2="6" y2="18"></line>
          <line x1="6" y1="6" x2="18" y2="18"></line>
        </svg>
      ` : `
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="3" y1="12" x2="21" y2="12"></line>
          <line x1="3" y1="6" x2="21" y2="6"></line>
          <line x1="3" y1="18" x2="21" y2="18"></line>
        </svg>
      `;
    });

    // Close when clicking any nav link
    navLinks.querySelectorAll('a').forEach(link => {
      link.addEventListener('click', () => {
        navLinks.classList.remove('mobile-open');
        toggleBtn.setAttribute('aria-expanded', 'false');
        toggleBtn.innerHTML = `
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="3" y1="12" x2="21" y2="12"></line>
            <line x1="3" y1="6" x2="21" y2="6"></line>
            <line x1="3" y1="18" x2="21" y2="18"></line>
          </svg>
        `;
      });
    });

    // Close on click outside
    document.addEventListener('click', (e) => {
      if (!navbar.contains(e.target)) {
        navLinks.classList.remove('mobile-open');
        toggleBtn.setAttribute('aria-expanded', 'false');
        toggleBtn.innerHTML = `
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="3" y1="12" x2="21" y2="12"></line>
            <line x1="3" y1="6" x2="21" y2="6"></line>
            <line x1="3" y1="18" x2="21" y2="18"></line>
          </svg>
        `;
      }
    });
  });
}

/**
 * Customer Portal / Dashboard Off-Canvas Sidebar
 * Provides off-canvas drawer toggle, dark backdrop, and swipe/escape dismissal
 */
function initDashboardSidebar() {
  const sidebar = document.querySelector('.dashboard-sidebar');
  const topbar = document.querySelector('.dashboard-topbar');
  if (!sidebar) return;

  // Ensure backdrop element exists in DOM
  let backdrop = document.querySelector('.sidebar-backdrop');
  if (!backdrop) {
    backdrop = document.createElement('div');
    backdrop.className = 'sidebar-backdrop';
    document.body.appendChild(backdrop);
  }

  // Ensure close button exists in sidebar header
  const sidebarHeader = sidebar.querySelector('.sidebar-header');
  if (sidebarHeader && !sidebarHeader.querySelector('.sidebar-close-btn')) {
    const closeBtn = document.createElement('button');
    closeBtn.className = 'sidebar-close-btn';
    closeBtn.setAttribute('aria-label', 'Close Sidebar');
    closeBtn.innerHTML = '&times;';
    sidebarHeader.appendChild(closeBtn);

    closeBtn.addEventListener('click', () => {
      closeSidebarDrawer();
    });
  }

  // Ensure sidebar toggle button exists in topbar
  if (topbar) {
    const topbarLeft = topbar.querySelector('.topbar-left');
    if (topbarLeft && !topbarLeft.querySelector('.sidebar-toggle-btn')) {
      const toggleBtn = document.createElement('button');
      toggleBtn.className = 'sidebar-toggle-btn';
      toggleBtn.setAttribute('aria-label', 'Toggle Navigation Menu');
      toggleBtn.innerHTML = `
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="3" y1="12" x2="21" y2="12"></line>
          <line x1="3" y1="6" x2="21" y2="6"></line>
          <line x1="3" y1="18" x2="21" y2="18"></line>
        </svg>
      `;
      topbarLeft.prepend(toggleBtn);

      toggleBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        toggleSidebarDrawer();
      });
    }
  }

  function toggleSidebarDrawer() {
    const isOpen = sidebar.classList.toggle('sidebar-open');
    if (isOpen) {
      backdrop.classList.add('active');
      document.body.style.overflow = 'hidden';
    } else {
      backdrop.classList.remove('active');
      document.body.style.overflow = '';
    }
  }

  function closeSidebarDrawer() {
    sidebar.classList.remove('sidebar-open');
    backdrop.classList.remove('active');
    document.body.style.overflow = '';
  }

  // Close when clicking backdrop
  backdrop.addEventListener('click', closeSidebarDrawer);

  // Close when clicking any sidebar item on mobile
  sidebar.querySelectorAll('.sidebar-item').forEach(link => {
    link.addEventListener('click', () => {
      if (window.innerWidth <= 900) {
        closeSidebarDrawer();
      }
    });
  });

  // Close on Escape key
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && sidebar.classList.contains('sidebar-open')) {
      closeSidebarDrawer();
    }
  });
}

