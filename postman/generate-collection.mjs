import { writeFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const directory = dirname(fileURLToPath(import.meta.url));

const testEvent = (expectedStatus, extra = []) => ({
  listen: "test",
  script: {
    type: "text/javascript",
    exec: [
      `pm.test("status is ${expectedStatus}", () => pm.response.to.have.status(${expectedStatus}));`,
      "pm.test(\"response time is below 10 seconds\", () => pm.expect(pm.response.responseTime).to.be.below(10000));",
      ...extra
    ]
  }
});

const request = ({ name, method = "GET", path, token, body, status = 200, tests = [] }) => {
  const headers = [];
  if (token) headers.push({ key: "Authorization", value: `Bearer {{${token}}}`, type: "text" });
  if (body !== undefined) headers.push({ key: "Content-Type", value: "application/json", type: "text" });
  const value = {
    name,
    event: [testEvent(status, tests)],
    request: {
      method,
      header: headers,
      url: `{{baseUrl}}${path}`
    }
  };
  if (body !== undefined) {
    value.request.body = { mode: "raw", raw: typeof body === "string" ? body : JSON.stringify(body, null, 2), options: { raw: { language: "json" } } };
  }
  return value;
};

const collection = {
  info: {
    _postman_id: "22ddc7ca-3f13-48ca-9e3d-34bcba8f9101",
    name: "Hobby SaaS API Acceptance",
    description: "Deterministic client-level acceptance suite. Run only against the isolated acceptance Compose environment; it creates data and expects Free/Plus fixtures.",
    schema: "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  event: [{
    listen: "prerequest",
    script: {
      type: "text/javascript",
      exec: [
        "if (!pm.collectionVariables.get('runId')) {",
        "  const now = new Date();",
        "  const runId = Date.now().toString().slice(-10);",
        "  const date = new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate()));",
        "  const from = new Date(date); from.setUTCDate(from.getUTCDate() - 7);",
        "  const customEnd = new Date(date); customEnd.setUTCDate(customEnd.getUTCDate() + 14);",
        "  pm.collectionVariables.set('runId', runId);",
        "  pm.collectionVariables.set('startedAt', new Date(Date.now() - 3600000).toISOString());",
        "  pm.collectionVariables.set('today', date.toISOString().slice(0, 10));",
        "  pm.collectionVariables.set('fromDate', from.toISOString().slice(0, 10));",
        "  pm.collectionVariables.set('toDate', date.toISOString().slice(0, 10));",
        "  pm.collectionVariables.set('customEndDate', customEnd.toISOString().slice(0, 10));",
        "  pm.collectionVariables.set('currentYear', String(now.getUTCFullYear()));",
        "  pm.collectionVariables.set('currentMonth', String(now.getUTCMonth() + 1));",
        "  pm.collectionVariables.set('freeUsername', `acceptance-free-${runId}`);",
        "  pm.collectionVariables.set('plusUsername', `acceptance-plus-${runId}`);",
        "}"
      ]
    }
  }],
  variable: [],
  item: [
    {
      name: "00 - Infrastructure and security",
      item: [
        request({ name: "Health is UP", path: "/actuator/health", tests: ["const body = pm.response.json();", "pm.test(\"health is UP\", () => pm.expect(body.status).to.eql('UP'));" ] }),
        request({ name: "OpenAPI exposes core contracts", path: "/v3/api-docs", tests: [
          "const body = pm.response.json();",
          "pm.test(\"core paths are documented\", () => ['/api/sessions','/api/me/gamification','/api/me/goals','/api/me/insights','/api/me/wrapped'].forEach(path => pm.expect(body.paths).to.have.property(path)));"
        ] }),
        request({ name: "Protected endpoint rejects missing bearer", path: "/api/me", status: 401 }),
        request({ name: "Protected endpoint rejects invalid bearer", path: "/api/me", token: "invalidToken", status: 401 })
      ]
    },
    {
      name: "01 - Users, catalog and hobbies",
      item: [
        request({ name: "Free user is provisioned", path: "/api/me", token: "freeToken", tests: [
          "const body = pm.response.json();",
          "pm.test(\"Free fixture identity is server-derived\", () => { pm.expect(body.id).to.eql('acceptance-free-user'); pm.expect(body.email).to.eql('free@example.test'); });"
        ] }),
        request({ name: "Plus user is provisioned", path: "/api/me", token: "plusToken", tests: [
          "const body = pm.response.json();",
          "pm.test(\"Plus fixture identity is server-derived\", () => pm.expect(body.id).to.eql('acceptance-plus-user'));"
        ] }),
        request({ name: "Update Free profile", method: "PATCH", path: "/api/me", token: "freeToken", body: {
          name: "Acceptance Free", bio: "Private acceptance profile", username: "{{freeUsername}}"
        }, tests: ["const body = pm.response.json();", "pm.test(\"username is normalized and persisted\", () => pm.expect(body.username).to.eql(pm.collectionVariables.get('freeUsername')));" ] }),
        request({ name: "Update Plus profile", method: "PATCH", path: "/api/me", token: "plusToken", body: {
          name: "Acceptance Plus", bio: "Public acceptance profile", username: "{{plusUsername}}"
        } }),
        request({ name: "List hobby catalog", path: "/api/hobbies", token: "freeToken", tests: [
          "const body = pm.response.json();",
          "const running = body.find(item => item.name === 'Running');",
          "pm.test(\"seeded catalog contains Running\", () => { pm.expect(body.length).to.be.at.least(10); pm.expect(running).to.exist; });",
          "pm.collectionVariables.set('hobbyId', running.id);"
        ] }),
        request({ name: "Templates require a linked hobby", path: "/api/hobbies/{{hobbyId}}/attribute-templates", token: "freeToken", status: 400 }),
        request({ name: "Add hobby to Free user", method: "POST", path: "/api/me/hobbies", token: "freeToken", status: 201, body: {
          hobbyId: "{{hobbyId}}", experienceLevel: "beginner"
        } }),
        request({ name: "Add hobby to Plus user", method: "POST", path: "/api/me/hobbies", token: "plusToken", status: 201, body: {
          hobbyId: "{{hobbyId}}", experienceLevel: "intermediate"
        } }),
        request({ name: "List Running attribute templates", path: "/api/hobbies/{{hobbyId}}/attribute-templates", token: "freeToken", tests: [
          "const body = pm.response.json();",
          "pm.test(\"dynamic attributes come from template\", () => pm.expect(body.map(item => item.key)).to.include('distance_km'));"
        ] }),
        request({ name: "Update Free hobby experience", method: "PATCH", path: "/api/me/hobbies/{{hobbyId}}", token: "freeToken", body: {
          experienceLevel: "learning"
        } }),
        request({ name: "List Free hobbies", path: "/api/me/hobbies", token: "freeToken", tests: [
          "const body = pm.response.json();", "pm.test(\"linked hobby is returned\", () => pm.expect(body).to.have.length(1));"
        ] }),
        request({ name: "Feature flags are authenticated and explicit", path: "/api/features", token: "freeToken", tests: [
          "const body = pm.response.json();",
          "pm.test(\"acceptance rollout flags are expected\", () => { pm.expect(body.gamification).to.eql(true); pm.expect(body.plusFeatures).to.eql(true); pm.expect(body.photoUploads).to.eql(false); pm.expect(body.sessionLocation).to.eql(false); });"
        ] })
      ]
    },
    {
      name: "02 - Equipment and backlog",
      item: [
        request({ name: "Create Free equipment", method: "POST", path: "/api/me/equipment", token: "freeToken", status: 201, body: {
          hobbyId: "{{hobbyId}}", category: "Shoes", name: "Acceptance Shoes"
        }, tests: ["const body = pm.response.json();", "pm.collectionVariables.set('freeEquipmentId', body.id);", "pm.test(\"equipment owns selected hobby\", () => pm.expect(body.hobbyId).to.eql(pm.collectionVariables.get('hobbyId')));" ] }),
        request({ name: "Create Plus equipment", method: "POST", path: "/api/me/equipment", token: "plusToken", status: 201, body: {
          hobbyId: "{{hobbyId}}", category: "Shoes", name: "Plus Shoes"
        }, tests: ["const body = pm.response.json();", "pm.collectionVariables.set('plusEquipmentId', body.id);" ] }),
        request({ name: "Update Free equipment", method: "PATCH", path: "/api/me/equipment/{{freeEquipmentId}}", token: "freeToken", body: {
          hobbyId: "{{hobbyId}}", category: "Shoes", name: "Acceptance Shoes Updated"
        } }),
        request({ name: "Free cannot update another user's equipment", method: "PATCH", path: "/api/me/equipment/{{plusEquipmentId}}", token: "freeToken", status: 404, body: {
          hobbyId: "{{hobbyId}}", category: "Shoes", name: "Forbidden update"
        } }),
        request({ name: "List Free equipment", path: "/api/me/equipment?hobbyId={{hobbyId}}", token: "freeToken", tests: ["const body = pm.response.json();", "pm.test(\"only owned equipment is listed\", () => { pm.expect(body).to.have.length(1); pm.expect(body[0].id).to.eql(pm.collectionVariables.get('freeEquipmentId')); });" ] }),
        request({ name: "Create Free basic backlog item", method: "POST", path: "/api/me/backlog-items", token: "freeToken", status: 201, body: {
          hobbyId: "{{hobbyId}}", title: "Run a first 5K", status: "pending"
        }, tests: ["const body = pm.response.json();", "pm.collectionVariables.set('freeBacklogId', body.id);", "pm.test(\"basic planning defaults are Free\", () => { pm.expect(body.priority).to.eql('normal'); pm.expect(body.archived).to.eql(false); });" ] }),
        request({ name: "Free advanced backlog is rejected", method: "POST", path: "/api/me/backlog-items", token: "freeToken", status: 403, body: {
          hobbyId: "{{hobbyId}}", title: "Advanced Free attempt", status: "pending", priority: "high", position: 1
        } }),
        request({ name: "Create Plus advanced backlog item", method: "POST", path: "/api/me/backlog-items", token: "plusToken", status: 201, body: {
          hobbyId: "{{hobbyId}}", title: "Plus race plan", status: "in_progress", dueDate: "{{customEndDate}}", priority: "high", archived: false, position: 2
        }, tests: ["const body = pm.response.json();", "pm.collectionVariables.set('plusBacklogId', body.id);", "pm.test(\"advanced fields are persisted\", () => { pm.expect(body.priority).to.eql('high'); pm.expect(body.position).to.eql(2); });" ] }),
        request({ name: "Free cannot update another user's backlog item", method: "PATCH", path: "/api/me/backlog-items/{{plusBacklogId}}", token: "freeToken", status: 404, body: {
          hobbyId: "{{hobbyId}}", title: "Forbidden", status: "pending"
        } }),
        request({ name: "List Plus backlog", path: "/api/me/backlog-items?hobbyId={{hobbyId}}", token: "plusToken", tests: ["const body = pm.response.json();", "pm.test(\"Plus item is listed\", () => pm.expect(body.some(item => item.id === pm.collectionVariables.get('plusBacklogId'))).to.eql(true));" ] })
      ]
    },
    {
      name: "03 - Sessions and visibility",
      item: [
        request({ name: "Create private Free session", method: "POST", path: "/api/sessions", token: "freeToken", status: 201, body: {
          hobbyId: "{{hobbyId}}", title: "Private acceptance run", startedAt: "{{startedAt}}", durationMinutes: 35, notes: "Internal reflection", satisfaction: 4, location: null, projectId: "{{freeBacklogId}}", equipmentIds: ["{{freeEquipmentId}}"], photos: [], attributes: { distance_km: 5.2, avg_pace_min_km: 6.7, surface: "road" }, visibility: "only_me"
        }, tests: ["const body = pm.response.json();", "pm.collectionVariables.set('freeSessionId', body.id);", "pm.test(\"session relations and visibility are returned\", () => { pm.expect(body.visibility).to.eql('only_me'); pm.expect(body.projectId).to.eql(pm.collectionVariables.get('freeBacklogId')); pm.expect(body.equipmentIds).to.include(pm.collectionVariables.get('freeEquipmentId')); });" ] }),
        request({ name: "Create public Plus session", method: "POST", path: "/api/sessions", token: "plusToken", status: 201, body: {
          hobbyId: "{{hobbyId}}", title: "Public acceptance run", startedAt: "{{startedAt}}", durationMinutes: 50, notes: "Public reflection", satisfaction: 5, location: null, projectId: "{{plusBacklogId}}", equipmentIds: ["{{plusEquipmentId}}"], photos: [], attributes: { distance_km: 8.1, avg_pace_min_km: 6.1, surface: "trail" }, visibility: "everyone"
        }, tests: ["const body = pm.response.json();", "pm.collectionVariables.set('plusSessionId', body.id);", "pm.test(\"public visibility is persisted\", () => pm.expect(body.visibility).to.eql('everyone'));" ] }),
        request({ name: "Get owned Free session", path: "/api/sessions/{{freeSessionId}}", token: "freeToken" }),
        request({ name: "Update owned Free session", method: "PATCH", path: "/api/sessions/{{freeSessionId}}", token: "freeToken", body: {
          hobbyId: "{{hobbyId}}", title: "Private acceptance run updated", startedAt: "{{startedAt}}", durationMinutes: 40, notes: "Updated reflection", satisfaction: 5, location: null, projectId: "{{freeBacklogId}}", equipmentIds: ["{{freeEquipmentId}}"], photos: [], attributes: { distance_km: 5.5, avg_pace_min_km: 6.5, surface: "road" }, visibility: "only_me"
        }, tests: ["const body = pm.response.json();", "pm.test(\"update replaces mutable fields\", () => { pm.expect(body.durationMinutes).to.eql(40); pm.expect(body.title).to.contain('updated'); });" ] }),
        request({ name: "List owned Free sessions", path: "/api/sessions?page=0&size=20", token: "freeToken", tests: ["const body = pm.response.json();", "pm.test(\"owner pagination contains private session\", () => pm.expect(body.items.some(item => item.id === pm.collectionVariables.get('freeSessionId'))).to.eql(true));" ] }),
        request({ name: "Free cannot read Plus session through owner endpoint", path: "/api/sessions/{{plusSessionId}}", token: "freeToken", status: 404 }),
        request({ name: "Free reads Plus public profile without sensitive fields", path: "/api/users/{{plusUsername}}", token: "freeToken", tests: [
          "const body = pm.response.json();",
          "pm.test(\"public DTO hides provider identity\", () => { pm.expect(body.username).to.eql(pm.collectionVariables.get('plusUsername')); pm.expect(body).not.to.have.property('id'); pm.expect(body).not.to.have.property('email'); });"
        ] }),
        request({ name: "Free lists Plus public sessions", path: "/api/users/{{plusUsername}}/sessions?page=0&size=20", token: "freeToken", tests: ["const body = pm.response.json();", "pm.test(\"everyone session is visible\", () => pm.expect(body.items.some(item => item.id === pm.collectionVariables.get('plusSessionId'))).to.eql(true));" ] }),
        request({ name: "Free reads Plus public session detail", path: "/api/users/{{plusUsername}}/sessions/{{plusSessionId}}", token: "freeToken", tests: [
          "const body = pm.response.json();", "pm.test(\"public session omits internal relations\", () => { pm.expect(body.id).to.eql(pm.collectionVariables.get('plusSessionId')); pm.expect(body).not.to.have.property('equipmentIds'); pm.expect(body).not.to.have.property('projectId'); });"
        ] }),
        request({ name: "Plus cannot see Free private session", path: "/api/users/{{freeUsername}}/sessions?page=0&size=20", token: "plusToken", tests: ["const body = pm.response.json();", "pm.test(\"only_me session is absent\", () => pm.expect(body.items.some(item => item.id === pm.collectionVariables.get('freeSessionId'))).to.eql(false));" ] }),
        request({ name: "Invalid visibility is rejected", method: "POST", path: "/api/sessions", token: "freeToken", status: 400, body: {
          hobbyId: "{{hobbyId}}", title: "Invalid visibility", startedAt: "{{startedAt}}", durationMinutes: 10, notes: null, satisfaction: 3, location: null, projectId: null, equipmentIds: [], photos: [], attributes: {}, visibility: "followers"
        } }),
        request({ name: "Disabled location fails safely", method: "POST", path: "/api/sessions", token: "freeToken", status: 503, body: {
          hobbyId: "{{hobbyId}}", title: "Disabled location", startedAt: "{{startedAt}}", durationMinutes: 10, notes: null, satisfaction: 3, location: { placeId: "acceptance-place" }, projectId: null, equipmentIds: [], photos: [], attributes: {}, visibility: "only_me"
        } }),
        request({ name: "Disabled photo upload fails safely", method: "POST", path: "/api/me/session-photos/upload-url", token: "freeToken", status: 503, body: {
          contentType: "image/jpeg", fileName: "acceptance.jpg", sizeBytes: 1024
        } })
      ]
    },
    {
      name: "04 - Gamification and plans",
      item: [
        request({ name: "Free plan is derived from database", path: "/api/me/plan", token: "freeToken", tests: ["const body = pm.response.json();", "pm.test(\"Free has no active entitlement\", () => { pm.expect(body.plan).to.eql('free'); pm.expect(body.active).to.eql(false); });" ] }),
        request({ name: "Plus plan is derived from fixture", path: "/api/me/plan", token: "plusToken", tests: ["const body = pm.response.json();", "pm.test(\"Plus entitlement is active\", () => { pm.expect(body.plan).to.eql('plus'); pm.expect(body.active).to.eql(true); });" ] }),
        request({ name: "Create Free weekly goal", method: "POST", path: "/api/me/goals", token: "freeToken", status: 201, body: {
          hobbyId: "{{hobbyId}}", name: "Two runs this week", metric: "sessions", targetValue: 2, cadence: "weekly", startDate: null, endDate: null
        }, tests: ["const body = pm.response.json();", "pm.collectionVariables.set('freeGoalId', body.id);", "pm.test(\"Free goal is basic weekly\", () => { pm.expect(body.advanced).to.eql(false); pm.expect(body.progress).to.be.at.least(1); });" ] }),
        request({ name: "Second overlapping Free goal requires Plus", method: "POST", path: "/api/me/goals", token: "freeToken", status: 403, body: {
          hobbyId: "{{hobbyId}}", name: "Duplicate weekly goal", metric: "minutes", targetValue: 60, cadence: "weekly", startDate: null, endDate: null
        } }),
        request({ name: "Create Plus custom global goal", method: "POST", path: "/api/me/goals", token: "plusToken", status: 201, body: {
          hobbyId: null, name: "Custom Plus challenge", metric: "minutes", targetValue: 100, cadence: "custom", startDate: "{{today}}", endDate: "{{customEndDate}}"
        }, tests: ["const body = pm.response.json();", "pm.collectionVariables.set('plusGoalId', body.id);", "pm.test(\"custom global goal is advanced\", () => pm.expect(body.advanced).to.eql(true));" ] }),
        request({ name: "List Free goals", path: "/api/me/goals", token: "freeToken", tests: ["const body = pm.response.json();", "pm.test(\"created goal is listed\", () => pm.expect(body.some(goal => goal.id === pm.collectionVariables.get('freeGoalId'))).to.eql(true));" ] }),
        request({ name: "Free gamification dashboard", path: "/api/me/gamification", token: "freeToken", tests: [
          "const body = pm.response.json();",
          "pm.test(\"session generates XP and badge\", () => { pm.expect(body.hobbies[0].xp).to.be.above(0); pm.expect(body.badges.some(badge => badge.key === 'first_session')).to.eql(true); pm.expect(body.records.longestSessionMinutes).to.eql(40); });"
        ] }),
        request({ name: "Plus gamification dashboard", path: "/api/me/gamification", token: "plusToken", tests: [
          "const body = pm.response.json();",
          "const badge = body.badges.find(item => item.key === 'first_session');",
          "pm.test(\"Plus earned first-session badge\", () => pm.expect(badge).to.exist);",
          "pm.collectionVariables.set('plusBadgeId', badge.id);"
        ] }),
        request({ name: "Free advanced insights require Plus", path: "/api/me/insights?from={{fromDate}}&to={{toDate}}", token: "freeToken", status: 403 }),
        request({ name: "Plus advanced insights", path: "/api/me/insights?from={{fromDate}}&to={{toDate}}", token: "plusToken", tests: ["const body = pm.response.json();", "pm.test(\"insights summarize current period\", () => { pm.expect(body.current.sessions).to.be.at.least(1); pm.expect(body.current.dailyActivity).to.be.an('array'); });" ] }),
        request({ name: "Plus Wrapped", path: "/api/me/wrapped?year={{currentYear}}&month={{currentMonth}}", token: "plusToken", tests: ["const body = pm.response.json();", "pm.test(\"Wrapped includes structured summary\", () => { pm.expect(body.period).to.eql('month'); pm.expect(body.summary.from.startsWith(pm.collectionVariables.get('currentYear'))).to.eql(true); pm.expect(body.summary.sessions).to.be.at.least(1); });" ] }),
        request({ name: "Current streak is available", path: "/api/me/streak", token: "freeToken", tests: ["const body = pm.response.json();", "pm.test(\"streak is derived from sessions\", () => pm.expect(body.currentStreakDays).to.be.at.least(1));" ] })
      ]
    },
    {
      name: "05 - Plus tools and portability",
      item: [
        request({ name: "Free profile customization is readable", path: "/api/me/profile-customization", token: "freeToken", tests: ["const body = pm.response.json();", "pm.test(\"default theme is readable\", () => { pm.expect(body.theme).to.eql('default'); pm.expect(body.supporterBadge).to.eql(false); });" ] }),
        request({ name: "Free cannot mutate Plus customization", method: "PATCH", path: "/api/me/profile-customization", token: "freeToken", status: 403, body: { theme: "forest", featuredBadgeIds: [] } }),
        request({ name: "Plus customizes profile with earned badge", method: "PATCH", path: "/api/me/profile-customization", token: "plusToken", body: { theme: "forest", featuredBadgeIds: ["{{plusBadgeId}}"] }, tests: ["const body = pm.response.json();", "pm.test(\"theme and owned badge are persisted\", () => { pm.expect(body.theme).to.eql('forest'); pm.expect(body.featuredBadges).to.have.length(1); pm.expect(body.supporterBadge).to.eql(true); });" ] }),
        request({ name: "Free cannot create maintenance rule", method: "POST", path: "/api/me/equipment-maintenance", token: "freeToken", status: 403, body: {
          equipmentId: "{{freeEquipmentId}}", name: "Rotate shoes", intervalMinutes: 300, lastMaintainedAt: null, active: true
        } }),
        request({ name: "Plus creates maintenance rule", method: "POST", path: "/api/me/equipment-maintenance", token: "plusToken", status: 201, body: {
          equipmentId: "{{plusEquipmentId}}", name: "Inspect shoes", intervalMinutes: 300, lastMaintainedAt: null, active: true
        }, tests: ["const body = pm.response.json();", "pm.collectionVariables.set('maintenanceRuleId', body.id);", "pm.test(\"usage is derived from linked session\", () => pm.expect(body.usedMinutes).to.be.at.least(50));" ] }),
        request({ name: "Plus completes maintenance", method: "POST", path: "/api/me/equipment-maintenance/{{maintenanceRuleId}}/complete", token: "plusToken", tests: ["const body = pm.response.json();", "pm.test(\"maintenance resets usage window\", () => { pm.expect(body.lastMaintainedAt).to.exist; pm.expect(body.usedMinutes).to.eql(0); });" ] }),
        request({ name: "Plus lists maintenance rules", path: "/api/me/equipment-maintenance", token: "plusToken", tests: ["const body = pm.response.json();", "pm.test(\"owned rule is listed\", () => pm.expect(body.some(rule => rule.id === pm.collectionVariables.get('maintenanceRuleId'))).to.eql(true));" ] }),
        request({ name: "Free JSON export contains only own data", path: "/api/me/export/json", token: "freeToken", tests: [
          "const body = pm.response.json();",
          "const serialized = JSON.stringify(body);",
          "pm.test(\"export is scoped and hides internal media keys\", () => { pm.expect(serialized).to.include('acceptance-free-user'); pm.expect(serialized).not.to.include('acceptance-plus-user'); pm.expect(serialized).not.to.include('storageKey'); });"
        ] }),
        request({ name: "Free CSV export is safe and non-empty", path: "/api/me/export/sessions.csv", token: "freeToken", tests: ["pm.test(\"CSV has header and own session\", () => { pm.expect(pm.response.text()).to.include('started_at'); pm.expect(pm.response.text()).to.include('Private acceptance run updated'); });" ] }),
        request({ name: "Archive Free goal", method: "DELETE", path: "/api/me/goals/{{freeGoalId}}", token: "freeToken", status: 204 }),
        request({ name: "Archive Plus goal", method: "DELETE", path: "/api/me/goals/{{plusGoalId}}", token: "plusToken", status: 204 }),
        request({ name: "Delete Plus maintenance rule", method: "DELETE", path: "/api/me/equipment-maintenance/{{maintenanceRuleId}}", token: "plusToken", status: 204 })
      ]
    }
  ]
};

writeFileSync(join(directory, "HobbySaaS.postman_collection.json"), `${JSON.stringify(collection, null, 2)}\n`);
