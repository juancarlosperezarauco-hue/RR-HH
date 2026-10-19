"""Development-only deterministic planner. It has no database or user JWT access."""

import hmac
import os
import re
from datetime import date
from typing import Annotated, Any, Literal

from fastapi import Depends, FastAPI, Header, HTTPException, Request
from fastapi.responses import JSONResponse
from pydantic import BaseModel, ConfigDict, Field, model_validator


class StrictModel(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)


class DatasetCapabilities(StrictModel):
    aggregate: bool
    detail: bool
    dimensions: list[str]
    metrics: list[str]


class PlannerRequest(StrictModel):
    contractVersion: Literal["1"]
    question: str = Field(min_length=1, max_length=2000)
    catalogVersion: Literal["1"]
    capabilities: dict[str, DatasetCapabilities]

    @model_validator(mode="after")
    def known_datasets(self):
        if not set(self.capabilities) <= {"employees.roster", "attendance.daily"}:
            raise ValueError("Unknown dataset")
        return self


class PlanResponse(StrictModel):
    status: Literal["query_plan"]
    plan: dict[str, Any]


class ClarificationResponse(StrictModel):
    status: Literal["needs_clarification"]
    question: str
    reasonCode: Literal["MISSING_DATE_RANGE"]


class UnsupportedResponse(StrictModel):
    status: Literal["unsupported"]
    message: str
    reasonCode: Literal[
        "DATASET_NOT_AVAILABLE", "CAPABILITY_NOT_AVAILABLE", "UNRECOGNIZED_REQUEST"
    ]


PlannerResponse = Annotated[
    PlanResponse | ClarificationResponse | UnsupportedResponse,
    Field(discriminator="status"),
]

app = FastAPI(docs_url=None, redoc_url=None, openapi_url=None)


@app.middleware("http")
async def bounded_request(request: Request, call_next):
    if request.url.path == "/internal/v1/plan" and len(await request.body()) > 32 * 1024:
        return JSONResponse(status_code=413, content={"detail": "Payload too large"})
    return await call_next(request)


def service_auth(
    token: Annotated[str | None, Header(alias="X-Internal-Service-Token")] = None,
) -> None:
    expected = os.getenv("AI_PLANNER_SERVICE_TOKEN", "")
    if len(expected) < 32:
        raise HTTPException(status_code=503, detail="Planner unavailable")
    if token is None or not hmac.compare_digest(token, expected):
        raise HTTPException(status_code=401, detail="Unauthorized")


def supports(
    request: PlannerRequest,
    dataset: str,
    mode: Literal["detail", "aggregate"],
    dimensions: set[str],
    metrics: set[str] = frozenset(),
) -> bool:
    available = request.capabilities.get(dataset)
    return bool(
        available
        and getattr(available, mode)
        and dimensions <= set(available.dimensions)
        and metrics <= set(available.metrics)
    )


def mock_plan(request: PlannerRequest) -> PlannerResponse:
    question = request.question.strip().casefold()
    employee = re.fullmatch(r"listar empleados al (\d{4}-\d{2}-\d{2})", question)
    if employee:
        try:
            date.fromisoformat(employee.group(1))
        except ValueError:
            return UnsupportedResponse(
                status="unsupported", message="La fecha no es válida.",
                reasonCode="UNRECOGNIZED_REQUEST",
            )
        if not supports(request, "employees.roster", "detail",
                        {"employee.full_name", "department.name"}):
            return UnsupportedResponse(
                status="unsupported", message="Capacidades insuficientes.",
                reasonCode="CAPABILITY_NOT_AVAILABLE",
            )
        return PlanResponse(status="query_plan", plan={
            "version": "1", "catalogVersion": "1", "dataset": "employees.roster",
            "mode": "detail", "asOfDate": employee.group(1),
            "select": ["employee.full_name", "department.name"], "metrics": [],
            "filters": [], "groupBy": [], "having": [],
            "orderBy": [{"field": "employee.full_name", "direction": "ASC"}],
            "page": {"number": 0, "size": 20}, "visualization": {"type": "table"},
        })

    delays = re.fullmatch(
        r"retrasos por departamento desde (\d{4}-\d{2}-\d{2}) "
        r"hasta antes de (\d{4}-\d{2}-\d{2})", question,
    )
    if delays:
        try:
            start, end = date.fromisoformat(delays.group(1)), date.fromisoformat(delays.group(2))
            if not 0 < (end - start).days <= 366:
                raise ValueError("Invalid range")
        except ValueError:
            return ClarificationResponse(
                status="needs_clarification", question="¿Qué rango de fechas necesitas?",
                reasonCode="MISSING_DATE_RANGE",
            )
        if not supports(request, "attendance.daily", "aggregate",
                        {"department.id", "department.name"},
                        {"attendance.late_days", "attendance.late_minutes_total"}):
            return UnsupportedResponse(
                status="unsupported", message="Capacidades insuficientes.",
                reasonCode="CAPABILITY_NOT_AVAILABLE",
            )
        return PlanResponse(status="query_plan", plan={
            "version": "1", "catalogVersion": "1", "dataset": "attendance.daily",
            "mode": "aggregate", "select": ["department.name"],
            "metrics": ["attendance.late_days", "attendance.late_minutes_total"],
            "dateRange": {"field": "attendance.work_date", "from": str(start),
                          "toExclusive": str(end)},
            "filters": [], "groupBy": ["department.id"], "having": [],
            "orderBy": [{"field": "attendance.late_days", "direction": "DESC"}],
            "page": {"number": 0, "size": 20}, "visualization": {"type": "table"},
        })

    if question == "retrasos por departamento":
        return ClarificationResponse(
            status="needs_clarification", question="¿Qué rango de fechas necesitas?",
            reasonCode="MISSING_DATE_RANGE",
        )
    if "nómina" in question or "nomina" in question:
        return UnsupportedResponse(
            status="unsupported", message="La nómina no está disponible.",
            reasonCode="DATASET_NOT_AVAILABLE",
        )
    return UnsupportedResponse(
        status="unsupported", message="Pregunta no reconocida por el simulador.",
        reasonCode="UNRECOGNIZED_REQUEST",
    )


@app.post("/internal/v1/plan", response_model=PlannerResponse)
def plan(request: PlannerRequest, _auth: Annotated[None, Depends(service_auth)]):
    if os.getenv("APP_ENV") not in {"development", "test"} or os.getenv("AI_PLANNER_MODE") != "mock":
        raise HTTPException(status_code=503, detail="Planner unavailable")
    return mock_plan(request)
