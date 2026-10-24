/**
 * Smart Civic Response System - Main Interactive Controller
 */

document.addEventListener('DOMContentLoaded', () => {
  App.init();
});

const App = {
  currentTab: 'citizen',
  issues: [],
  currentTrackedIssue: null,
  triageDebounceTimer: null,
  activeFilters: {
    category: 'ALL',
    status: 'ALL',
    priority: 'ALL',
    q: ''
  },
  photoBase64: null,

  init() {
    this.bindNavigation();
    this.bindCitizenForm();
    this.bindLiveFeedFilters();
    this.bindAdminTable();
    this.bindLocationPicker();
    this.bindTrackerSearch();
    this.bindPresetSamples();
    
    // Initial data load
    this.refreshAllData();

    // Auto refresh stats and feed every 20 seconds
    setInterval(() => {
      this.refreshFeedAndStats();
    }, 20000);
  },

  // --------------------------------------------------------------------------
  // Navigation & Tab Switching
  // --------------------------------------------------------------------------
  bindNavigation() {
    const tabButtons = document.querySelectorAll('.nav-tab-btn');
    tabButtons.forEach(btn => {
      btn.addEventListener('click', () => {
        const target = btn.dataset.tab;
        this.switchTab(target);
      });
    });

    const sosBtn = document.getElementById('btnEmergencySos');
    if (sosBtn) {
      sosBtn.addEventListener('click', () => {
        this.switchTab('citizen');
        const descEl = document.getElementById('issueDescription');
        const titleEl = document.getElementById('issueTitle');
        if (titleEl) titleEl.value = 'URGENT: Hazardous Open Manhole & Gas Odor Near Residential Complex';
        if (descEl) {
          descEl.value = 'Emergency situation: Cast iron sewer lid missing, strong fumes escaping, pedestrians and children walking right past. Immediate barricading and repair required!';
          this.triggerAiTriage();
        }
        titleEl.focus();
        this.showToast('Emergency SOS template loaded into report form. Please verify location.', 'info');
      });
    }
  },

  switchTab(tabId) {
    this.currentTab = tabId;
    document.querySelectorAll('.nav-tab-btn').forEach(btn => {
      btn.classList.toggle('active', btn.dataset.tab === tabId);
    });

    document.querySelectorAll('.view-section').forEach(view => {
      view.style.display = view.id === `view-${tabId}` ? 'block' : 'none';
    });

    if (tabId === 'feed') {
      this.loadLiveFeed();
    } else if (tabId === 'admin') {
      this.loadAdminDashboard();
    } else if (tabId === 'departments') {
      this.loadDepartments();
    }
  },

  // --------------------------------------------------------------------------
  // Global Data Refresh
  // --------------------------------------------------------------------------
  async refreshAllData() {
    await Promise.all([
      this.loadLiveFeed(),
      this.loadAdminStats(),
      this.loadDepartments()
    ]);
  },

  async refreshFeedAndStats() {
    try {
      const stats = await CivicApi.getStats();
      this.renderHeroStats(stats);
      if (this.currentTab === 'admin') {
        this.renderAdminStats(stats);
      }
    } catch (e) {
      console.warn('Background refresh error', e);
    }
  },

  renderHeroStats(stats) {
    if (!stats) return;
    const totalEl = document.getElementById('heroTotalIssues');
    const rateEl = document.getElementById('heroResolutionRate');
    const avgEl = document.getElementById('heroAvgHours');
    const slaEl = document.getElementById('heroSlaActive');

    if (totalEl) totalEl.textContent = stats.totalIssues ?? '--';
    if (rateEl) rateEl.textContent = `${stats.resolutionRatePercent ?? 85}%`;
    if (avgEl) avgEl.textContent = `${stats.avgResolutionHours ?? 22}h`;
    if (slaEl) slaEl.textContent = `${stats.criticalCount ?? 0} Critical`;
  },

  // --------------------------------------------------------------------------
  // Citizen Complaint Form & Live Smart AI Triage
  // --------------------------------------------------------------------------
  bindCitizenForm() {
    const titleInput = document.getElementById('issueTitle');
    const descInput = document.getElementById('issueDescription');
    const form = document.getElementById('reportIssueForm');

    // Debounced real-time triage
    const handleTyping = () => {
      clearTimeout(this.triageDebounceTimer);
      this.triageDebounceTimer = setTimeout(() => {
        this.triggerAiTriage();
      }, 350);
    };

    if (titleInput) titleInput.addEventListener('input', handleTyping);
    if (descInput) descInput.addEventListener('input', handleTyping);

    // Form Submission
    if (form) {
      form.addEventListener('submit', async (e) => {
        e.preventDefault();
        await this.handleIssueSubmit();
      });
    }

    // Photo Dropzone
    const dropzone = document.getElementById('photoDropzone');
    const fileInput = document.getElementById('photoFileInput');
    const removeBtn = document.getElementById('btnRemovePhoto');

    if (dropzone && fileInput) {
      dropzone.addEventListener('click', () => fileInput.click());

      dropzone.addEventListener('dragover', (e) => {
        e.preventDefault();
        dropzone.classList.add('dragover');
      });

      dropzone.addEventListener('dragleave', () => {
        dropzone.classList.remove('dragover');
      });

      dropzone.addEventListener('drop', (e) => {
        e.preventDefault();
        dropzone.classList.remove('dragover');
        if (e.dataTransfer.files && e.dataTransfer.files[0]) {
          this.processImageFile(e.dataTransfer.files[0]);
        }
      });

      fileInput.addEventListener('change', () => {
        if (fileInput.files && fileInput.files[0]) {
          this.processImageFile(fileInput.files[0]);
        }
      });
    }

    if (removeBtn) {
      removeBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        this.clearPhoto();
      });
    }
  },

  processImageFile(file) {
    if (!file.type.startsWith('image/')) {
      this.showToast('Please upload a valid image file (PNG, JPG, WebP)', 'error');
      return;
    }
    const reader = new FileReader();
    reader.onload = (e) => {
      this.photoBase64 = e.target.result;
      this.showPhotoPreview(this.photoBase64);
      this.showToast('Photo attached to complaint', 'info');
    };
    reader.readAsDataURL(file);
  },

  showPhotoPreview(url) {
    const wrap = document.getElementById('photoPreviewWrap');
    const img = document.getElementById('photoPreviewImg');
    if (wrap && img) {
      img.src = url;
      wrap.classList.add('active');
    }
  },

  clearPhoto() {
    this.photoBase64 = null;
    const wrap = document.getElementById('photoPreviewWrap');
    const img = document.getElementById('photoPreviewImg');
    const fileInput = document.getElementById('photoFileInput');
    if (wrap) wrap.classList.remove('active');
    if (img) img.src = '';
    if (fileInput) fileInput.value = '';
  },

  bindPresetSamples() {
    document.querySelectorAll('.btn-preset').forEach(btn => {
      btn.addEventListener('click', () => {
        const sampleType = btn.dataset.sample;
        this.loadSampleData(sampleType);
      });
    });
  },

  loadSampleData(type) {
    const samples = {
      pothole: {
        title: 'Deep 2-Foot Asphalt Crater Causing Traffic Skidding',
        desc: 'Dangerous pothole has opened up right before the junction turn. Vehicles are swerving into oncoming traffic to avoid it. High risk during evening darkness.',
        photo: 'https://images.unsplash.com/photo-1515162816999-a0c47dc192f7?auto=format&fit=crop&w=800&q=80',
        ward: 'Ward 7 - North Enclave',
        address: 'Outer Ring Road, Junction 4'
      },
      water: {
        title: 'Emergency High Pressure Water Pipeline Burst',
        desc: 'Drinking water transmission line ruptured. Huge fountain of water flooding surrounding street and entering ground floor shops. Contamination risk.',
        photo: 'https://images.unsplash.com/photo-1541888946425-d0fbb18086f6?auto=format&fit=crop&w=800&q=80',
        ward: 'Ward 12 - Midtown Sector',
        address: '5th Avenue, Behind Metro Pillar 88'
      },
      garbage: {
        title: 'Severe Municipal Dump Overflowing onto Sidewalk',
        desc: 'Large community garbage container overflowing for 5 days. Foul smell, rotting food, stray dogs scattering waste across pedestrian lane.',
        photo: 'https://images.unsplash.com/photo-1605600659908-0ef719419d41?auto=format&fit=crop&w=800&q=80',
        ward: 'Ward 15 - West Market',
        address: 'Sector 15 Market Lane, near Gate 2'
      },
      electric: {
        title: 'Sparking Transformer Pole Low Hanging Live Cable',
        desc: 'Electrical transformer sparking with popping sound. An uninsulated wire has snapped and is dangling 6 feet above pedestrian sidewalk.',
        photo: 'https://images.unsplash.com/photo-1473341304170-971dccb5ac1e?auto=format&fit=crop&w=800&q=80',
        ward: 'Ward 4 - East Colony',
        address: 'Krishna Nagar, Crossroad 9'
      }
    };

    const s = samples[type];
    if (!s) return;

    document.getElementById('issueTitle').value = s.title;
    document.getElementById('issueDescription').value = s.desc;
    document.getElementById('issueAddress').value = s.address;
    document.getElementById('issueWard').value = s.ward;
    
    this.photoBase64 = s.photo;
    this.showPhotoPreview(s.photo);

    this.triggerAiTriage();
    this.showToast(`Sample preset '${type}' populated!`, 'info');
  },

  async triggerAiTriage() {
    const title = document.getElementById('issueTitle').value;
    const desc = document.getElementById('issueDescription').value;

    if (!title && !desc) return;

    const triageCard = document.getElementById('aiTriageCard');
    if (triageCard) triageCard.classList.add('analyzing');

    try {
      const result = await CivicApi.triage(title, desc);
      if (result) {
        this.renderTriageResult(result);
      }
    } catch (err) {
      console.warn('AI Triage error:', err);
    } finally {
      if (triageCard) triageCard.classList.remove('analyzing');
    }
  },

  renderTriageResult(result) {
    const catPill = document.getElementById('aiTriageCat');
    const priorityPill = document.getElementById('aiTriagePriority');
    const deptPill = document.getElementById('aiTriageDept');
    const slaPill = document.getElementById('aiTriageSla');
    const rationalePill = document.getElementById('aiTriageRationale');

    if (catPill) catPill.textContent = result.category || 'ROADS';
    if (priorityPill) {
      priorityPill.textContent = `${result.priority} (${result.urgencyScore}/100)`;
      priorityPill.style.color = result.priorityColor || '#f59e0b';
    }
    if (deptPill) deptPill.textContent = result.department || 'Roads Infrastructure';
    if (slaPill) slaPill.textContent = `${result.slaHours}h SLA`;
    if (rationalePill) rationalePill.textContent = result.rationale || 'Auto-classified';
  },

  // --------------------------------------------------------------------------
  // Mini GIS Location & GPS
  // --------------------------------------------------------------------------
  bindLocationPicker() {
    const locateBtn = document.getElementById('btnDetectGps');
    const mapPicker = document.getElementById('miniMapPicker');
    const pin = document.getElementById('miniMapPin');
    const coordsDisplay = document.getElementById('miniMapCoords');

    let currentLat = 28.6139;
    let currentLng = 77.2090;

    const updatePin = (xPercent, yPercent, lat, lng) => {
      if (pin) {
        pin.style.left = `${xPercent}%`;
        pin.style.top = `${yPercent}%`;
      }
      currentLat = lat;
      currentLng = lng;
      if (coordsDisplay) {
        coordsDisplay.textContent = `GPS: ${lat.toFixed(4)}, ${lng.toFixed(4)}`;
      }
      const latInput = document.getElementById('issueLatitude');
      const lngInput = document.getElementById('issueLongitude');
      if (latInput) latInput.value = lat;
      if (lngInput) lngInput.value = lng;
    };

    if (mapPicker) {
      mapPicker.addEventListener('click', (e) => {
        const rect = mapPicker.getBoundingClientRect();
        const x = e.clientX - rect.left;
        const y = e.clientY - rect.top;
        const xPct = Math.max(5, Math.min(95, (x / rect.width) * 100));
        const yPct = Math.max(5, Math.min(95, (y / rect.height) * 100));

        // Simulated delta coordinates around city center
        const lat = 28.6139 + (0.5 - yPct / 100) * 0.05;
        const lng = 77.2090 + (xPct / 100 - 0.5) * 0.05;

        updatePin(xPct, yPct, lat, lng);
      });
    }

    if (locateBtn) {
      locateBtn.addEventListener('click', () => {
        if (!navigator.geolocation) {
          this.showToast('Geolocation is not supported by your browser.', 'error');
          return;
        }

        locateBtn.innerHTML = 'Detecting...';
        navigator.geolocation.getCurrentPosition(
          (pos) => {
            locateBtn.innerHTML = 'GPS Located';
            const lat = pos.coords.latitude;
            const lng = pos.coords.longitude;
            updatePin(50, 50, lat, lng);
            const addrInput = document.getElementById('issueAddress');
            if (addrInput && !addrInput.value) {
              addrInput.value = `Geo Pin (${lat.toFixed(4)}, ${lng.toFixed(4)})`;
            }
            this.showToast('Exact GPS coordinates captured!', 'success');
          },
          (err) => {
            locateBtn.innerHTML = 'Use City Center GPS';
            updatePin(50, 50, 28.6139, 77.2090);
            this.showToast('Permission not granted; city center coordinates applied.', 'info');
          },
          { timeout: 7000 }
        );
      });
    }
  },

  // --------------------------------------------------------------------------
  // Issue Submission Handler
  // --------------------------------------------------------------------------
  async handleIssueSubmit() {
    const submitBtn = document.getElementById('btnSubmitIssue');
    const title = document.getElementById('issueTitle').value.trim();
    const desc = document.getElementById('issueDescription').value.trim();
    const catSelect = document.getElementById('issueCategory').value;
    const prioritySelect = document.getElementById('issuePriority').value;
    const address = document.getElementById('issueAddress').value.trim();
    const ward = document.getElementById('issueWard').value;
    const name = document.getElementById('reporterName').value.trim();
    const phone = document.getElementById('reporterPhone').value.trim();
    const email = document.getElementById('reporterEmail').value.trim();

    const latVal = parseFloat(document.getElementById('issueLatitude')?.value) || 28.6139;
    const lngVal = parseFloat(document.getElementById('issueLongitude')?.value) || 77.2090;

    if (!title) {
      this.showToast('Please provide an issue title.', 'error');
      return;
    }

    submitBtn.disabled = true;
    submitBtn.innerHTML = 'Submitting & Dispatching...';

    const payload = {
      title,
      description: desc,
      category: catSelect,
      priority: prioritySelect,
      address: address || 'Main Thoroughfare',
      ward: ward || 'Ward 12 - Midtown Sector',
      latitude: latVal,
      longitude: lngVal,
      reporterName: name || 'Citizen Resident',
      reporterPhone: phone || '+91 98000 00000',
      reporterEmail: email,
      photoUrl: this.photoBase64 || 'https://images.unsplash.com/photo-1515162816999-a0c47dc192f7?auto=format&fit=crop&w=800&q=80'
    };

    try {
      const created = await CivicApi.createIssue(payload);
      this.showToast(`Issue Registered: ${created.id}`, 'success');

      // Reset Form
      document.getElementById('reportIssueForm').reset();
      this.clearPhoto();

      // Refresh data
      await this.refreshAllData();

      // Show in Tracker view on the right
      this.renderTrackerIssue(created);

    } catch (err) {
      this.showToast(`Submission failed: ${err.message}`, 'error');
    } finally {
      submitBtn.disabled = false;
      submitBtn.innerHTML = 'Register Civic Issue';
    }
  },

  // --------------------------------------------------------------------------
  // Tracker View & Search
  // --------------------------------------------------------------------------
  bindTrackerSearch() {
    const trackInput = document.getElementById('trackIssueIdInput');
    const trackBtn = document.getElementById('btnTrackSearch');

    const doSearch = async () => {
      const id = trackInput.value.trim();
      if (!id) {
        this.showToast('Please enter a Ticket ID (e.g., CIV-2026-1001)', 'error');
        return;
      }
      try {
        const issue = await CivicApi.getIssue(id);
        this.renderTrackerIssue(issue);
        this.showToast(`Ticket ${id} found`, 'success');
      } catch (e) {
        this.showToast(`Ticket not found: ${id}`, 'error');
      }
    };

    if (trackBtn) trackBtn.addEventListener('click', doSearch);
    if (trackInput) {
      trackInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
          e.preventDefault();
          doSearch();
        }
      });
    }
  },

  renderTrackerIssue(issue) {
    this.currentTrackedIssue = issue;
    const container = document.getElementById('trackerResultsContainer');
    if (!container) return;

    if (!issue) {
      container.innerHTML = `<p class="panel-desc" style="text-align: center; padding: 2rem 0;">Enter a Ticket ID above to track live progress.</p>`;
      return;
    }

    const priorityColors = {
      CRITICAL: 'badge-critical',
      HIGH: 'badge-high',
      MEDIUM: 'badge-medium',
      LOW: 'badge-low'
    };

    const statusBadgeClass = priorityColors[issue.priority] || 'badge-medium';

    // Build timeline steps
    const steps = [
      { key: 'REPORTED', title: '1. Incident Reported' },
      { key: 'TRIAGED', title: '2. Triaged & Assigned' },
      { key: 'IN_PROGRESS', title: '3. Field Action In Progress' },
      { key: 'RESOLVED', title: '4. Issue Resolved' },
      { key: 'CLOSED', title: '5. Case Closed' }
    ];

    const currentOrder = this.getStatusOrder(issue.status);

    let stepperHtml = '';
    steps.forEach((step, idx) => {
      const stepOrder = idx + 1;
      let stateClass = '';
      if (stepOrder < currentOrder) {
        stateClass = 'completed';
      } else if (stepOrder === currentOrder) {
        stateClass = 'current';
      }

      // Find matching timeline event
      const evt = issue.timeline ? issue.timeline.find(t => t.status === step.key) : null;

      stepperHtml += `
        <div class="timeline-step ${stateClass}">
          <div class="step-marker">${stepOrder < currentOrder ? '✓' : stepOrder}</div>
          <div class="step-content">
            <div class="step-title-row">
              <span class="step-title">${step.title}</span>
              <span class="step-time">${evt ? this.formatTime(evt.timestamp) : (stepOrder < currentOrder ? 'Completed' : 'Pending')}</span>
            </div>
            ${evt ? `
              <div class="step-actor">${evt.actor || ''} - ${evt.action || ''}</div>
              <div class="step-note">${evt.note || ''}</div>
            ` : (stepOrder === currentOrder ? `<div class="step-note">Currently under active operational review.</div>` : '')}
          </div>
        </div>
      `;
    });

    container.innerHTML = `
      <div style="background: rgba(15, 23, 42, 0.7); border: 1px solid var(--border-subtle); border-radius: var(--radius-md); padding: 1.25rem; margin-bottom: 1.25rem;">
        <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 0.75rem;">
          <div>
            <span class="table-id" style="font-size: 1.1rem;">${issue.id}</span>
            <h4 style="color: #fff; margin-top: 0.35rem; font-size: 1.05rem;">${issue.title}</h4>
          </div>
          <span class="badge ${statusBadgeClass}">${issue.priorityLabel || issue.priority}</span>
        </div>
        <p style="font-size: 0.85rem; color: var(--text-muted); margin-bottom: 0.85rem;">${issue.description}</p>
        
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 0.5rem; font-size: 0.78rem; color: var(--text-dim); border-top: 1px solid var(--border-subtle); padding-top: 0.75rem;">
          <div><strong style="color: var(--text-main);">Department:</strong> ${issue.department || 'General'}</div>
          <div><strong style="color: var(--text-main);">Ward:</strong> ${issue.ward || 'General'}</div>
          <div><strong style="color: var(--text-main);">Status:</strong> <span style="color: var(--cyan-400);">${issue.statusLabel || issue.status}</span></div>
          <div><strong style="color: var(--text-main);">Officer:</strong> ${issue.assignedOfficer || 'Pending dispatch'}</div>
        </div>

        ${issue.photoUrl ? `
          <div style="margin-top: 0.85rem;">
            <img src="${issue.photoUrl}" alt="Issue photo" style="width: 100%; max-height: 160px; object-fit: cover; border-radius: var(--radius-sm); border: 1px solid var(--border-subtle);" />
          </div>
        ` : ''}

        <div class="upvote-container">
          <span style="font-size: 0.85rem; color: var(--text-muted);">
            Affected too? <strong style="color: #fff;">${issue.upvotes || 0} citizens</strong> endorsed
          </span>
          <button class="btn-upvote" onclick="App.handleUpvote('${issue.id}')">
            ▲ Endorse (${issue.upvotes || 0})
          </button>
        </div>
      </div>

      <h5 style="color: #fff; margin-bottom: 0.75rem; font-size: 0.95rem;">Live Resolution Progress</h5>
      <div class="timeline-stepper">
        ${stepperHtml}
      </div>
    `;
  },

  getStatusOrder(status) {
    switch (status) {
      case 'REPORTED': return 1;
      case 'TRIAGED': return 2;
      case 'IN_PROGRESS': return 3;
      case 'RESOLVED': return 4;
      case 'CLOSED': return 5;
      default: return 1;
    }
  },

  // --------------------------------------------------------------------------
  // Public Live Feed & GIS Radar View
  // --------------------------------------------------------------------------
  bindLiveFeedFilters() {
    const catPills = document.querySelectorAll('.cat-filter-pill');
    catPills.forEach(pill => {
      pill.addEventListener('click', () => {
        catPills.forEach(p => p.classList.remove('active'));
        pill.classList.add('active');
        this.activeFilters.category = pill.dataset.cat;
        this.loadLiveFeed();
      });
    });

    const statusPills = document.querySelectorAll('.status-filter-pill');
    statusPills.forEach(pill => {
      pill.addEventListener('click', () => {
        statusPills.forEach(p => p.classList.remove('active'));
        pill.classList.add('active');
        this.activeFilters.status = pill.dataset.status;
        this.loadLiveFeed();
      });
    });

    const searchInput = document.getElementById('feedSearchInput');
    if (searchInput) {
      searchInput.addEventListener('input', () => {
        this.activeFilters.q = searchInput.value.trim();
        clearTimeout(this.searchDebounce);
        this.searchDebounce = setTimeout(() => {
          this.loadLiveFeed();
        }, 300);
      });
    }
  },

  async loadLiveFeed() {
    const container = document.getElementById('issuesFeedContainer');
    if (!container) return;

    try {
      const issues = await CivicApi.getIssues(this.activeFilters);
      this.issues = issues;
      this.renderFeedCards(issues);
      this.renderGisRadarPins(issues);
      this.renderQuickPicks(issues);
    } catch (e) {
      container.innerHTML = `<div style="grid-column: 1/-1; text-align: center; padding: 3rem; color: var(--rose-400);">Failed to load civic issues feed: ${e.message}</div>`;
    }
  },

  renderFeedCards(issues) {
    const container = document.getElementById('issuesFeedContainer');
    if (!container) return;

    if (!issues || issues.length === 0) {
      container.innerHTML = `
        <div style="grid-column: 1/-1; text-align: center; padding: 4rem 1rem; color: var(--text-muted);">
          <div style="font-size: 2.5rem; margin-bottom: 0.5rem;">🔍</div>
          <h4>No matching civic incidents found</h4>
          <p style="font-size: 0.85rem; color: var(--text-dim); margin-top: 0.25rem;">Try adjusting category or search filters.</p>
        </div>
      `;
      return;
    }

    container.innerHTML = issues.map(issue => {
      const priorityClass = `priority-${issue.priority || 'MEDIUM'}`;
      const badgeClass = issue.priority === 'CRITICAL' ? 'badge-critical' :
                         issue.priority === 'HIGH' ? 'badge-high' :
                         issue.priority === 'LOW' ? 'badge-low' : 'badge-medium';

      const isBreached = issue.isSlaBreached;
      const remainingSla = issue.remainingSlaHours ?? issue.slaHours;

      return `
        <article class="issue-card ${priorityClass}">
          <img src="${issue.photoUrl || 'https://images.unsplash.com/photo-1515162816999-a0c47dc192f7?auto=format&fit=crop&w=800&q=80'}"
               alt="${issue.title}"
               class="issue-card-media"
               loading="lazy" />
          <div class="issue-card-body">
            <div class="issue-card-badges">
              <span class="badge ${badgeClass}">${issue.priorityLabel || issue.priority}</span>
              <span class="badge badge-status">${issue.statusLabel || issue.status}</span>
            </div>

            <h3 class="issue-card-title">${issue.title}</h3>
            <p class="issue-card-desc">${issue.description}</p>

            <div class="issue-card-meta">
              <div class="issue-meta-item">
                <span style="color: var(--cyan-400);">📍</span> ${issue.address || 'Location on record'}
              </div>
              <div class="issue-meta-item">
                <span style="color: var(--amber-400);">🏛️</span> ${issue.department || 'Municipal Board'}
              </div>
              <div class="issue-meta-item">
                <span style="color: var(--text-dim);">⏱️</span> Reported: ${this.formatTime(issue.createdAt)}
              </div>
            </div>

            <div class="issue-card-footer">
              <span class="sla-indicator ${isBreached ? 'breached' : 'ok'}">
                ${isBreached ? '⚠️ SLA Breached' : `⏳ SLA: ${remainingSla}h left`}
              </span>
              <div class="card-actions">
                <button class="btn-card-action" onclick="App.handleUpvote('${issue.id}')" title="Endorse this issue">
                  ▲ ${issue.upvotes || 0}
                </button>
                <button class="btn-card-action" onclick="App.trackIssueFromFeed('${issue.id}')">
                  Timeline
                </button>
              </div>
            </div>
          </div>
        </article>
      `;
    }).join('');
  },

  renderGisRadarPins(issues) {
    const radarContainer = document.getElementById('gisRadarPins');
    if (!radarContainer) return;

    radarContainer.innerHTML = issues.slice(0, 8).map((issue, idx) => {
      // Pseudo positions across radar circle
      const angle = (idx / Math.min(issues.length, 8)) * 2 * Math.PI;
      const radius = 35 + (idx % 3) * 12; // percent
      const x = 50 + radius * Math.cos(angle);
      const y = 50 + radius * Math.sin(angle);

      const color = issue.priority === 'CRITICAL' ? 'var(--rose-500)' :
                    issue.priority === 'HIGH' ? 'var(--amber-500)' : 'var(--cyan-400)';

      return `
        <div class="radar-pin" style="position: absolute; left: ${x}%; top: ${y}%; transform: translate(-50%, -50%); cursor: pointer; z-index: 5;"
             onclick="App.openIssueDetailsModal('${issue.id}')"
             title="${issue.id}: ${issue.title}">
          <div style="width: 14px; height: 14px; background: ${color}; border: 2px solid #fff; border-radius: 50%; box-shadow: 0 0 10px ${color};"></div>
          <span style="background: rgba(0,0,0,0.85); color: #fff; font-size: 0.65rem; padding: 2px 5px; border-radius: 4px; position: absolute; top: -18px; left: 50%; transform: translateX(-50%); white-space: nowrap;">
            ${issue.id}
          </span>
        </div>
      `;
    }).join('');
  },

  renderQuickPicks(issues) {
    const container = document.getElementById('quickPicksRow');
    if (!container || !issues) return;

    container.innerHTML = issues.slice(0, 5).map(issue => `
      <button class="quick-pick-btn" onclick="App.trackIssueFromFeed('${issue.id}')">
        ${issue.id}
      </button>
    `).join('');
  },

  trackIssueFromFeed(id) {
    this.switchTab('citizen');
    const input = document.getElementById('trackIssueIdInput');
    if (input) input.value = id;
    const issue = this.issues.find(i => i.id === id);
    if (issue) {
      this.renderTrackerIssue(issue);
    } else {
      CivicApi.getIssue(id).then(res => this.renderTrackerIssue(res));
    }
  },

  async handleUpvote(issueId) {
    try {
      const resp = await CivicApi.upvote(issueId);
      this.showToast(`Issue ${issueId} endorsed (+1 vote)`, 'success');
      await this.refreshAllData();
      if (this.currentTrackedIssue && this.currentTrackedIssue.id === issueId) {
        const updated = await CivicApi.getIssue(issueId);
        this.renderTrackerIssue(updated);
      }
    } catch (e) {
      this.showToast('Upvote failed: ' + e.message, 'error');
    }
  },

  // --------------------------------------------------------------------------
  // Municipal Admin Command Center
  // --------------------------------------------------------------------------
  bindAdminTable() {
    const refreshBtn = document.getElementById('btnRefreshAdmin');
    if (refreshBtn) {
      refreshBtn.addEventListener('click', () => {
        this.loadAdminDashboard();
        this.showToast('Admin data refreshed', 'info');
      });
    }
  },

  async loadAdminDashboard() {
    await Promise.all([
      this.loadAdminStats(),
      this.loadAdminTableIssues()
    ]);
  },

  async loadAdminStats() {
    try {
      const stats = await CivicApi.getStats();
      this.renderHeroStats(stats);
      this.renderAdminStats(stats);
    } catch (e) {
      console.warn('Failed to load stats', e);
    }
  },

  renderAdminStats(stats) {
    if (!stats) return;
    const setVal = (id, val) => {
      const el = document.getElementById(id);
      if (el) el.textContent = val ?? '--';
    };

    setVal('kpiTotalIssues', stats.totalIssues);
    setVal('kpiPendingIssues', stats.activePending);
    setVal('kpiInProgress', stats.inProgressCount);
    setVal('kpiResolvedIssues', `${stats.resolvedCount} (${stats.resolutionRatePercent}%)`);
    setVal('kpiBreachedIssues', stats.slaBreachedCount);
  },

  async loadAdminTableIssues() {
    const tbody = document.getElementById('adminIssuesTableBody');
    if (!tbody) return;

    try {
      const issues = await CivicApi.getIssues();
      if (!issues || issues.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" style="text-align: center; padding: 2rem;">No civic records in municipal registry.</td></tr>`;
        return;
      }

      tbody.innerHTML = issues.map(issue => {
        const priorityBadge = issue.priority === 'CRITICAL' ? 'badge-critical' :
                              issue.priority === 'HIGH' ? 'badge-high' :
                              issue.priority === 'LOW' ? 'badge-low' : 'badge-medium';

        return `
          <tr>
            <td>
              <span class="table-id">${issue.id}</span>
              <div style="font-size: 0.72rem; color: var(--text-dim);">${this.formatTime(issue.createdAt)}</div>
            </td>
            <td>
              <div style="font-weight: 600; color: #fff; max-width: 260px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                ${issue.title}
              </div>
              <div style="font-size: 0.76rem; color: var(--text-muted);">${issue.ward || 'General Ward'}</div>
            </td>
            <td><span class="badge ${priorityBadge}">${issue.priority}</span></td>
            <td><span style="font-size: 0.8rem; color: var(--text-main);">${issue.department || 'Unassigned'}</span></td>
            <td>
              <span class="sla-indicator ${issue.isSlaBreached ? 'breached' : 'ok'}">
                ${issue.isSlaBreached ? '⚠️ BREACHED' : `⏳ ${issue.remainingSlaHours ?? issue.slaHours}h left`}
              </span>
            </td>
            <td>
              <select class="table-status-select" onchange="App.handleInlineStatusChange('${issue.id}', this.value)">
                <option value="REPORTED" ${issue.status === 'REPORTED' ? 'selected' : ''}>REPORTED</option>
                <option value="TRIAGED" ${issue.status === 'TRIAGED' ? 'selected' : ''}>TRIAGED</option>
                <option value="IN_PROGRESS" ${issue.status === 'IN_PROGRESS' ? 'selected' : ''}>IN PROGRESS</option>
                <option value="RESOLVED" ${issue.status === 'RESOLVED' ? 'selected' : ''}>RESOLVED</option>
                <option value="CLOSED" ${issue.status === 'CLOSED' ? 'selected' : ''}>CLOSED</option>
              </select>
            </td>
            <td>
              <button class="btn-card-action" onclick="App.openManageModal('${issue.id}')">
                Manage
              </button>
            </td>
          </tr>
        `;
      }).join('');

    } catch (e) {
      tbody.innerHTML = `<tr><td colspan="7" style="color: var(--rose-400); text-align: center; padding: 2rem;">Error loading table: ${e.message}</td></tr>`;
    }
  },

  async handleInlineStatusChange(issueId, newStatus) {
    try {
      await CivicApi.updateStatus(issueId, {
        status: newStatus,
        actor: 'Admin Console Operator',
        notes: `Quick status updated to ${newStatus}`
      });
      this.showToast(`Issue ${issueId} updated to ${newStatus}`, 'success');
      await this.refreshAllData();
    } catch (e) {
      this.showToast('Status update failed: ' + e.message, 'error');
    }
  },

  // --------------------------------------------------------------------------
  // Department Overview View
  // --------------------------------------------------------------------------
  async loadDepartments() {
    const container = document.getElementById('departmentsGrid');
    if (!container) return;

    try {
      const depts = await CivicApi.getDepartments();
      container.innerHTML = depts.map(dept => `
        <div class="dept-card">
          <div class="dept-header">
            <div class="dept-icon">🏢</div>
            <div>
              <h4 class="dept-title">${dept.name}</h4>
              <div class="dept-officer">${dept.headOfficer}</div>
            </div>
          </div>

          <div class="dept-stats-grid">
            <div class="dept-stat-box">
              <div class="dept-stat-val" style="color: var(--amber-400);">${dept.pendingIssues || 0}</div>
              <div class="dept-stat-lbl">Active Pending</div>
            </div>
            <div class="dept-stat-box">
              <div class="dept-stat-val" style="color: var(--emerald-400);">${dept.resolvedIssues || 0}</div>
              <div class="dept-stat-lbl">Resolved</div>
            </div>
          </div>

          <div class="dept-contact">
            <div><strong>Nodal Contact:</strong> ${dept.phone}</div>
            <div><strong>Email:</strong> ${dept.contactEmail}</div>
            <div><strong>Field Force:</strong> ${dept.activeOfficers} Field Officers</div>
          </div>
        </div>
      `).join('');
    } catch (e) {
      container.innerHTML = `<div style="grid-column: 1/-1; text-align: center; padding: 2rem;">Failed to load departments: ${e.message}</div>`;
    }
  },

  // --------------------------------------------------------------------------
  // Modals & Action Handlers
  // --------------------------------------------------------------------------
  async openManageModal(issueId) {
    const issue = await CivicApi.getIssue(issueId);
    if (!issue) return;

    const modal = document.getElementById('manageTicketModal');
    const content = document.getElementById('manageModalBody');

    content.innerHTML = `
      <div style="margin-bottom: 1.25rem;">
        <span class="table-id" style="font-size: 1.15rem;">${issue.id}</span>
        <h3 style="color: #fff; margin-top: 0.35rem;">${issue.title}</h3>
        <p style="font-size: 0.85rem; color: var(--text-muted);">${issue.description}</p>
      </div>

      <div class="form-group">
        <label class="form-label">Workflow Status</label>
        <select id="modalStatusSelect" class="form-control">
          <option value="REPORTED" ${issue.status === 'REPORTED' ? 'selected' : ''}>REPORTED</option>
          <option value="TRIAGED" ${issue.status === 'TRIAGED' ? 'selected' : ''}>TRIAGED</option>
          <option value="IN_PROGRESS" ${issue.status === 'IN_PROGRESS' ? 'selected' : ''}>IN PROGRESS</option>
          <option value="RESOLVED" ${issue.status === 'RESOLVED' ? 'selected' : ''}>RESOLVED</option>
          <option value="CLOSED" ${issue.status === 'CLOSED' ? 'selected' : ''}>CLOSED</option>
        </select>
      </div>

      <div class="form-group">
        <label class="form-label">Assign Field Engineer / Officer</label>
        <input type="text" id="modalOfficerInput" class="form-control" value="${issue.assignedOfficer || ''}" placeholder="e.g. Officer Vinod Sharma (Rapid Squad 3)" />
      </div>

      <div class="form-group">
        <label class="form-label">Operational Dispatch Notes</label>
        <textarea id="modalNotesInput" class="form-control" placeholder="Describe actions taken, barricading completed, or maintenance schedule..."></textarea>
      </div>

      <div style="display: flex; justify-content: flex-end; gap: 0.75rem; margin-top: 1.5rem;">
        <button class="btn-card-action" onclick="App.closeModals()">Cancel</button>
        <button class="btn-submit-issue" style="width: auto; padding: 0.65rem 1.4rem;" onclick="App.saveManageModal('${issue.id}')">
          Save Action
        </button>
      </div>
    `;

    modal.classList.add('active');
  },

  async saveManageModal(issueId) {
    const status = document.getElementById('modalStatusSelect').value;
    const officer = document.getElementById('modalOfficerInput').value.trim();
    const notes = document.getElementById('modalNotesInput').value.trim();

    try {
      await CivicApi.updateStatus(issueId, {
        status,
        officer,
        notes: notes || `Admin updated status to ${status}`,
        actor: 'Municipal Executive'
      });

      this.showToast(`Ticket ${issueId} successfully updated!`, 'success');
      this.closeModals();
      await this.refreshAllData();
    } catch (e) {
      this.showToast('Save failed: ' + e.message, 'error');
    }
  },

  openIssueDetailsModal(issueId) {
    this.trackIssueFromFeed(issueId);
  },

  closeModals() {
    document.querySelectorAll('.modal-backdrop').forEach(m => m.classList.remove('active'));
  },

  // --------------------------------------------------------------------------
  // Helpers & Toast Notifications
  // --------------------------------------------------------------------------
  showToast(message, type = 'info') {
    const container = document.getElementById('toastContainer');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    const icon = type === 'success' ? '✅' : type === 'error' ? '❌' : 'ℹ️';
    toast.innerHTML = `<span>${icon}</span> <span>${message}</span>`;

    container.appendChild(toast);

    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transform = 'translateX(100%)';
      setTimeout(() => toast.remove(), 300);
    }, 4000);
  },

  formatTime(isoString) {
    if (!isoString) return 'Just now';
    try {
      const date = new Date(isoString);
      return date.toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch (e) {
      return isoString;
    }
  }
};

window.App = App;
