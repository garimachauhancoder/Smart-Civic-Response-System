/**
 * Smart Civic Response System - Pure REST API Client
 */

const CivicApi = {
  baseUrl: '/api',

  async request(endpoint, options = {}) {
    const url = `${this.baseUrl}${endpoint}`;
    const isFormData = typeof FormData !== 'undefined' && options.body instanceof FormData;
    const defaultHeaders = {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };

    const config = {
      ...options,
      headers: {
        ...defaultHeaders,
        ...(options.headers || {})
      }
    };

    if (isFormData) {
      delete config.headers['Content-Type'];
    } else if (config.body && typeof config.body === 'object') {
      config.body = JSON.stringify(config.body);
    }

    try {
      const response = await fetch(url, config);
      const text = await response.text();
      let data = {};
      try {
        data = text ? JSON.parse(text) : {};
      } catch (err) {
        data = { message: text };
      }

      if (!response.ok) {
        const errorMsg = data.error || data.message || `HTTP ${response.status}`;
        throw new Error(errorMsg);
      }

      return data;
    } catch (err) {
      console.error(`[CivicApi] Request failed for ${endpoint}:`, err);
      throw err;
    }
  },

  async getHealth() {
    return this.request('/health');
  },

  async getStats() {
    return this.request('/stats');
  },

  async getDepartments() {
    return this.request('/departments');
  },

  async getOfficerIssues(ward = 'ALL') {
    return this.request(`/officer/issues?ward=${encodeURIComponent(ward)}`);
  },

  async triage(title, description) {
    return this.request('/triage', {
      method: 'POST',
      body: { title, description }
    });
  },

  async getIssues(params = {}) {
    const query = new URLSearchParams();
    if (params.category && params.category !== 'ALL') query.set('category', params.category);
    if (params.status && params.status !== 'ALL') query.set('status', params.status);
    if (params.priority && params.priority !== 'ALL') query.set('priority', params.priority);
    if (params.q) query.set('q', params.q);

    const qs = query.toString() ? `?${query.toString()}` : '';
    return this.request(`/issues${qs}`);
  },

  async getIssue(id) {
    return this.request(`/issues/${encodeURIComponent(id)}`);
  },

  async createIssue(issueData) {
    return this.request('/issues', {
      method: 'POST',
      body: issueData
    });
  },

  async updateStatus(id, updateData) {
    return this.request(`/issues/${encodeURIComponent(id)}/status`, {
      method: 'PUT',
      body: updateData
    });
  },

  async assignDepartment(id, assignData) {
    return this.request(`/issues/${encodeURIComponent(id)}/assign`, {
      method: 'PUT',
      body: assignData
    });
  },

  async upvote(id) {
    return this.request(`/issues/${encodeURIComponent(id)}/upvote`, {
      method: 'POST'
    });
  }
};

window.CivicApi = CivicApi;
