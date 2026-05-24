# QA Report: 주택 계약 위험도 진단 프론트엔드

Status: COMPLETE

Result: PASS

## QA Decision

- Decision: PASS
- Suggested loop target: none
- Basis: required command gates passed, real backend integration passed through create -> upload -> findings -> market price -> analyze -> report -> checklist, and the `:8080 wrong / :8081 real` proxy fallback worked in both dev and preview mode

## Environment

- Repo: `/Users/jeongcool/me/no-way-home`
- Branch: `feat/house-risk-agent-prompts/frontend`
- Date: `2026-05-24`
- Real database: PostgreSQL 18 via `docker compose`
- Real backend: Spring Boot `./gradlew bootRun --args='--server.port=8081'`
- Wrong upstream simulation: dummy HTTP server on `127.0.0.1:8080` returning `{"service":"not-no-way-home"}`
- Frontend dev under test: `http://127.0.0.1:5173`
- Frontend preview spot-check: `http://127.0.0.1:4173`
- Teardown: stopped preview/dev/backend/dummy sessions, closed Playwright browser session, ran `docker compose down`, verified no listeners remained on `5173`, `4173`, `8080`, `8081`, `5432`

## Commands Executed

```text
docker compose up -d postgres

node -e 'require("http").createServer((req,res)=>{res.writeHead(200,{"Content-Type":"application/json"});res.end(JSON.stringify({service:"not-no-way-home",port:8080,path:req.url}))}).listen(8080,"127.0.0.1")'

SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/no_way_home \
SPRING_DATASOURCE_USERNAME=no_way_home \
SPRING_DATASOURCE_PASSWORD=no_way_home \
./gradlew bootRun --args='--server.port=8081'

cd frontend && npm run dev -- --host 127.0.0.1 --port 5173

cd frontend && npm test
- pass
- vitest: 2 files passed, 10 tests passed

cd frontend && npm run build
- pass
- type checks passed
- vite production build passed

./gradlew test --tests com.nowayhome.housecheck.api.HouseCheckControllerIntegrationTest
- pass from Gradle cache

./gradlew test --tests com.nowayhome.housecheck.api.HouseCheckControllerIntegrationTest --rerun-tasks
- pass
- XML result: 4 tests, 0 failures, 0 errors
- evidence: build/test-results/test/TEST-com.nowayhome.housecheck.api.HouseCheckControllerIntegrationTest.xml

cd frontend && npm run preview -- --host 127.0.0.1 --port 4173

curl -i http://127.0.0.1:8080/api/status
- pass
- confirmed wrong-service response, not `no-way-home`

curl -X POST http://127.0.0.1:4173/api/house-checks ...
- pass
- preview proxy returned 200 with real `checkId`

docker compose down
```

## Executed Checks

- Dev proxy fallback passed.
  - `npm run dev` logged: `Detected backend origin http://localhost:8081 for /api proxy`
  - `curl http://127.0.0.1:8080/api/status` returned `service=not-no-way-home`
  - Browser request `POST http://127.0.0.1:5173/api/house-checks` returned `200` and real `checkId=8e304258-639e-472e-885a-2497b4d2fad5`

- Preview proxy fallback passed.
  - `npm run preview` logged: `Detected backend origin http://localhost:8081 for /api proxy`
  - `POST http://127.0.0.1:4173/api/house-checks` returned `200` and real `checkId=a7d565d4-9a22-4903-abab-55bb8f55e3d5`

- Full real-backend happy path passed in browser on dev.
  - Create: `POST /api/house-checks` -> `200`
  - Registry PDF upload: `POST /api/house-checks/{checkId}/registry-file` -> `200`
  - Building ledger PDF upload: `POST /api/house-checks/{checkId}/building-ledger-file` -> `200`
  - Registry findings save: `PUT /api/house-checks/{checkId}/registry-findings` -> `200`
  - Building ledger findings save: `PUT /api/house-checks/{checkId}/building-ledger-findings` -> `200`
  - Market price save: `POST /api/house-checks/{checkId}/market-price` -> `200`
  - Analyze: `POST /api/house-checks/{checkId}/analyze` -> `200`
  - Report auto-load: `GET /api/house-checks/{checkId}/report` -> `200`
  - Checklist auto-load: `GET /api/house-checks/{checkId}/checklist` -> `200`
  - Network evidence from Playwright request log showed the exact sequence above with status `200`

- Browser-visible result rendering passed.
  - Report tab showed `CAUTION / 주의`, core risk reason, registry/building/deposit/recovery sections, and source memo
  - Checklist tab showed all 3 stages: `계약 전`, `계약 직전`, `계약 후`

- Validation and boundary checks passed.
  - Before applying `User ID`, `진단 시작` and downstream actions were disabled
  - Non-PDF upload attempt showed `PDF 파일만 업로드할 수 있습니다.`
  - Empty market save showed validation for market value/jeonse value, source memo, and reference date
  - `sessionStorage` after the run contained only `house-risk-agent-prompts.user-id=owner-a`
  - `localStorage` was empty

- Access boundary behavior passed.
  - Changing `User ID` from `owner-a` to `owner-b` with the same `checkId` immediately cleared visible server-derived result content and showed the boundary reset banner
  - `결과 다시 불러오기` under `owner-b` triggered real backend `403` responses for `report` and `checklist`
  - UI switched to the dedicated `접근이 거부되었습니다` panel and did not show prior report/checklist content
  - Recovery actions worked: `User ID 다시 적용` removed the denied panel state, and `새 진단 시작` cleared `checkId`

## Evidence

- Access denied screenshot: [qa-access-denied.png](/Users/jeongcool/me/no-way-home/doc/agent-work/house-risk-agent-prompts-frontend/assets/qa-access-denied.png)
- Runtime controller integration result: [TEST-com.nowayhome.housecheck.api.HouseCheckControllerIntegrationTest.xml](/Users/jeongcool/me/no-way-home/build/test-results/test/TEST-com.nowayhome.housecheck.api.HouseCheckControllerIntegrationTest.xml)
- QA input files used for real uploads:
  - [qa-registry.pdf](/Users/jeongcool/me/no-way-home/doc/agent-work/house-risk-agent-prompts-frontend/assets/qa-registry.pdf)
  - [qa-building-ledger.pdf](/Users/jeongcool/me/no-way-home/doc/agent-work/house-risk-agent-prompts-frontend/assets/qa-building-ledger.pdf)
  - [qa-not-pdf.txt](/Users/jeongcool/me/no-way-home/doc/agent-work/house-risk-agent-prompts-frontend/assets/qa-not-pdf.txt)

## Defects

- None.

## Notes On Observed Errors

- During the deliberate `ACCESS_DENIED` reproduction, the browser console recorded `403` resource failures for `report` and `checklist`.
- This is expected for the test scenario and matched the intended UI state transition to the access-denied panel. It is not recorded as a defect.

## Residual Risks

- Preview mode was verified for the proxy fallback and create request, but not for the entire browser happy path. The full end-to-end browser flow was executed in Vite dev mode only.
- This QA run did not independently inspect encrypted-at-rest document bytes or database column encryption during the live browser scenario; that coverage remains in `HouseCheckControllerIntegrationTest`.
