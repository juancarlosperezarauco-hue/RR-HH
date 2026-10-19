from fastapi.testclient import TestClient
import pytest

from app.main import app


TOKEN = "synthetic-service-token-32-characters-minimum"
client = TestClient(app)


@pytest.fixture(autouse=True)
def configured(monkeypatch):
    monkeypatch.setenv("APP_ENV", "test")
    monkeypatch.setenv("AI_PLANNER_MODE", "mock")
    monkeypatch.setenv("AI_PLANNER_SERVICE_TOKEN", TOKEN)


def request(question: str):
    return {
        "contractVersion": "1", "question": question, "catalogVersion": "1",
        "capabilities": {
            "employees.roster": {
                "aggregate": True, "detail": True,
                "dimensions": ["employee.full_name", "department.name"],
                "metrics": ["employees.distinct_count"],
            },
            "attendance.daily": {
                "aggregate": True, "detail": False,
                "dimensions": ["department.id", "department.name"],
                "metrics": ["attendance.late_days", "attendance.late_minutes_total"],
            },
        },
    }


def post(body, token=TOKEN):
    return client.post("/internal/v1/plan", json=body,
                       headers={"X-Internal-Service-Token": token})


def test_two_plans_are_bounded_by_capabilities():
    detail = post(request("listar empleados al 2026-01-15"))
    assert detail.status_code == 200
    assert set(detail.json()) == {"status", "plan"}
    assert detail.json()["plan"]["dataset"] == "employees.roster"
    assert detail.json()["plan"]["asOfDate"] == "2026-01-15"

    aggregate = post(request(
        "retrasos por departamento desde 2026-01-01 hasta antes de 2026-03-01"))
    assert aggregate.status_code == 200
    assert aggregate.json()["plan"]["dataset"] == "attendance.daily"
    assert aggregate.json()["plan"]["groupBy"] == ["department.id"]

    restricted = request("listar empleados al 2026-01-15")
    restricted["capabilities"]["employees.roster"]["dimensions"].remove("employee.full_name")
    assert post(restricted).json()["reasonCode"] == "CAPABILITY_NOT_AVAILABLE"


def test_clarification_and_unsupported_variants():
    clarification = post(request("retrasos por departamento"))
    assert set(clarification.json()) == {"status", "question", "reasonCode"}
    assert clarification.json()["status"] == "needs_clarification"
    unsupported = post(request("reporte de nómina"))
    assert set(unsupported.json()) == {"status", "message", "reasonCode"}
    assert unsupported.json()["reasonCode"] == "DATASET_NOT_AVAILABLE"


def test_service_auth_and_controlled_environment(monkeypatch):
    assert client.post("/internal/v1/plan", json=request("retrasos por departamento")).status_code == 401
    assert post(request("retrasos por departamento"), token="wrong").status_code == 401
    monkeypatch.delenv("AI_PLANNER_SERVICE_TOKEN")
    assert post(request("retrasos por departamento")).status_code == 503
    monkeypatch.setenv("AI_PLANNER_SERVICE_TOKEN", TOKEN)
    monkeypatch.setenv("APP_ENV", "production")
    assert post(request("retrasos por departamento")).status_code == 503


def test_strict_request_and_size_limit():
    extra = request("retrasos por departamento")
    extra["sql"] = "SELECT *"
    assert post(extra).status_code == 422
    nested = request("retrasos por departamento")
    nested["capabilities"]["attendance.daily"]["schema"] = "public"
    assert post(nested).status_code == 422
    assert client.post("/internal/v1/plan", content=b"{broken",
                       headers={"Content-Type": "application/json",
                                "X-Internal-Service-Token": TOKEN}).status_code == 422
    assert post(request("x" * 33_000)).status_code == 413
