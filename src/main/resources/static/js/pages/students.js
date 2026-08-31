/**
 * Students management page — admin-only view for registering,
 * searching, and managing student accounts.
 */
const StudentsPage = (() => {
    let allStudents = [];

    async function render(container) {
        container.innerHTML = `
            <div class="page-header">
                <h1>Student Management</h1>
                <p class="subtitle">Register and manage student library accounts</p>
            </div>
            <div class="page-body">
                <div class="toolbar">
                    <div class="search-bar">
                        <span class="search-icon">🔍</span>
                        <input class="form-input" id="student-search"
                               placeholder="Search students..." type="search">
                    </div>
                    <div class="toolbar-actions">
                        <button class="btn btn-primary" id="register-btn">Register Student</button>
                    </div>
                </div>
                <div id="students-table">
                    <div class="loading-center"><div class="spinner"></div></div>
                </div>
            </div>
        `;

        document.getElementById('register-btn').addEventListener('click', showRegisterDialog);
        document.getElementById('student-search').addEventListener('input', (e) => {
            const q = e.target.value.toLowerCase();
            if (!q) { renderTable(allStudents); return; }
            renderTable(allStudents.filter(s =>
                (s.firstName || '').toLowerCase().includes(q) ||
                (s.lastName || '').toLowerCase().includes(q) ||
                (s.registrationNumber || '').toLowerCase().includes(q) ||
                (s.username || '').toLowerCase().includes(q)
            ));
        });

        await loadStudents();
    }

    async function loadStudents() {
        try {
            allStudents = await API.get('/students');
            renderTable(allStudents);
        } catch (e) {
            Toast.error('Failed to load students: ' + e.message);
        }
    }

    function renderTable(students) {
        const target = document.getElementById('students-table');
        if (students.length === 0) {
            target.innerHTML = `
                <div class="empty-state">
                    <div class="icon">👥</div>
                    <h3>No students found</h3>
                    <p>Register a new student to get started</p>
                </div>
            `;
            return;
        }

        target.innerHTML = `
            <div class="table-wrapper">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Name</th>
                            <th>Username</th>
                            <th>Reg. No.</th>
                            <th>Department</th>
                            <th>Semester</th>
                            <th>Status</th>
                            <th>Borrows</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${students.map(s => `
                            <tr>
                                <td><strong>${esc(s.firstName)} ${esc(s.lastName || '')}</strong></td>
                                <td class="text-muted">${esc(s.username)}</td>
                                <td>${esc(s.registrationNumber)}</td>
                                <td>${esc(s.department || '-')}</td>
                                <td>${s.semester || '-'}</td>
                                <td>${memberBadge(s.membershipStatus)}</td>
                                <td>${s.currentBorrowCount} / ${s.borrowLimit}</td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;
    }

    function showRegisterDialog() {
        const form = document.createElement('div');
        form.innerHTML = `
            <div id="reg-error" class="hidden" style="
                background: var(--red-bg);
                border: 1px solid rgba(196,77,56,0.2);
                border-radius: var(--radius-sm);
                padding: 10px 14px;
                margin-bottom: 14px;
                font-size: 0.8125rem;
                color: var(--red);
            "></div>
            <div class="form-group">
                <label class="form-label">First Name *</label>
                <input class="form-input" id="reg-fn" placeholder="First name">
            </div>
            <div class="form-group">
                <label class="form-label">Last Name *</label>
                <input class="form-input" id="reg-ln" placeholder="Last name">
            </div>
            <div class="form-group">
                <label class="form-label">Email *</label>
                <input class="form-input" id="reg-em" type="email" placeholder="student@university.edu">
            </div>
            <div class="form-group">
                <label class="form-label">Phone</label>
                <input class="form-input" id="reg-ph" placeholder="Phone number">
            </div>
            <div class="form-group">
                <label class="form-label">Department *</label>
                <input class="form-input" id="reg-dp" placeholder="e.g. Computer Science">
            </div>
            <div class="form-group">
                <label class="form-label">Course *</label>
                <input class="form-input" id="reg-co" placeholder="e.g. B.Tech">
            </div>
            <div class="form-group">
                <label class="form-label">Semester</label>
                <input class="form-input" id="reg-sm" type="number" min="1" max="12" value="1">
            </div>
            <div class="form-group">
                <label class="form-label">Section</label>
                <input class="form-input" id="reg-sc" placeholder="e.g. A">
            </div>
        `;

        Modal.open({
            title: 'Register New Student',
            content: form,
            actions: [
                { label: 'Cancel', onClick: () => Modal.close() },
                { label: 'Register', cls: 'btn-primary', onClick: handleRegister }
            ]
        });
    }

    async function handleRegister() {
        const fn = document.getElementById('reg-fn').value.trim();
        const ln = document.getElementById('reg-ln').value.trim();
        const em = document.getElementById('reg-em').value.trim();
        const dp = document.getElementById('reg-dp').value.trim();
        const co = document.getElementById('reg-co').value.trim();
        const errBox = document.getElementById('reg-error');

        // Clear previous error
        errBox.classList.add('hidden');
        errBox.textContent = '';

        if (!fn || !ln || !em || !dp || !co) {
            errBox.textContent = 'Please fill in all required fields (marked with *).';
            errBox.classList.remove('hidden');
            return;
        }

        try {
            const result = await API.post('/students', {
                firstName: fn,
                lastName: ln,
                email: em,
                phone: document.getElementById('reg-ph').value.trim(),
                department: dp,
                course: co,
                semester: parseInt(document.getElementById('reg-sm').value) || 1,
                section: document.getElementById('reg-sc').value.trim()
            });

            Modal.close();

            // Show credentials in a new modal
            Modal.open({
                title: 'Registration Successful',
                content: `
                    <p style="margin-bottom:16px">Share these credentials with the student:</p>
                    <div class="profile-grid" style="grid-template-columns:1fr">
                        <div class="profile-field">
                            <div class="profile-field-label">Username</div>
                            <div class="profile-field-value">${esc(result.username)}</div>
                        </div>
                        <div class="profile-field">
                            <div class="profile-field-label">Registration Number</div>
                            <div class="profile-field-value">${esc(result.registrationNumber)}</div>
                        </div>
                        <div class="profile-field">
                            <div class="profile-field-label">Default Password</div>
                            <div class="profile-field-value">${esc(result.defaultPassword)}</div>
                        </div>
                    </div>
                    <p class="text-muted mt-md" style="font-size:0.8125rem">
                        Student should change the password after first login.
                    </p>
                `,
                actions: [
                    { label: 'Done', cls: 'btn-primary', onClick: () => Modal.close() }
                ]
            });

            await loadStudents();
        } catch (e) {
            // Show error inline in the modal so user can fix the issue
            const msg = e.message || 'Failed to register student';
            errBox.textContent = msg;
            errBox.classList.remove('hidden');
            Toast.error(msg);
        }
    }

    function memberBadge(status) {
        if (status === 'ACTIVE')    return '<span class="badge badge-green">Active</span>';
        if (status === 'SUSPENDED') return '<span class="badge badge-red">Suspended</span>';
        if (status === 'EXPIRED')   return '<span class="badge badge-orange">Expired</span>';
        return `<span class="badge badge-muted">${status || '-'}</span>`;
    }

    function esc(s) { return (s || '').replace(/</g, '&lt;').replace(/>/g, '&gt;'); }

    return { render };
})();
