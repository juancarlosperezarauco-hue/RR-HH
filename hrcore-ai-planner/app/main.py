"""Authenticated internal planner. It has no database or user JWT access."""

import hmac
import os
import re
from datetime import date
from typing import Annotated, Any, Literal

from fastapi import Depends, FastAPI, Header, HTTPException, Request
from fastapi.responses import JSONResponse
from pydantic import BaseModel, ConfigDict, Field, TypeAdapter, ValidationError, field_validator, model_validator

from app.gemini_planner import GeminiPlanner, PlannerFailure


class StrictModel(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)


class DatasetCapabilities(StrictModel):
    aggregate: bool
    detail: bool
    dimensions: list[str]
    metrics: list[str]


class DimensionMetadata(StrictModel):
    type: Literal["UUID", "TEXT", "DATE", "LOCAL_TIME", "ENUM", "INTEGER"]
    operators: list[str]
    nullable: bool
    groupKey: bool
    labelFor: str


class MetricMetadata(StrictModel):
    grain: str


class TemporalRelation(StrictModel):
    path: str
    effectiveAt: str
    cardinality: str
    rule: str


class DatasetMetadata(StrictModel):
    grain: str
    dimensions: dict[str, DimensionMetadata]
    metrics: dict[str, MetricMetadata]
    temporalRelations: list[TemporalRelation]


class PlanLimits(StrictModel):
    maxPageSize: int
    maxDateRangeDays: int
    maxSelect: int
    maxMetrics: int
    maxFilters: int
    maxGroupBy: int
    maxHaving: int
    maxOrderBy: int


class SemanticContext(StrictModel):
    datasets: dict[str, DatasetMetadata]
    limits: PlanLimits
    businessDate: str | None = None
    businessTimezone: str | None = None

    @model_validator(mode="after")
    def business_clock(self):
        if (self.businessDate is None) != (self.businessTimezone is None):
            raise ValueError("Business date and timezone must be supplied together")
        if self.businessTimezone:
            if not re.fullmatch(r"[A-Za-z_]+(?:/[A-Za-z_]+)+", self.businessTimezone):
                raise ValueError("Invalid business timezone")
        if self.businessDate:
            try:
                if date.fromisoformat(self.businessDate).isoformat() != self.businessDate:
                    raise ValueError("Invalid business date")
            except ValueError as error:
                raise ValueError("Invalid business date") from error
        return self


class PlannerRequest(StrictModel):
    contractVersion: Literal["1"]
    question: str = Field(min_length=1, max_length=2000)
    catalogVersion: Literal["1"]
    capabilities: dict[str, DatasetCapabilities]
    semanticContext: SemanticContext | None = None

    @model_validator(mode="after")
    def valid_capabilities(self):
        if any(not re.fullmatch(r"[a-z][a-z0-9_]*(?:\.[a-z][a-z0-9_]*)+", dataset_id)
               for dataset_id in self.capabilities):
            raise ValueError("Invalid dataset identifier")
        if self.semanticContext:
            if set(self.semanticContext.datasets) != set(self.capabilities):
                raise ValueError("Metadata must match authorized datasets")
            for dataset_id, metadata in self.semanticContext.datasets.items():
                allowed = self.capabilities[dataset_id]
                if not set(metadata.dimensions) <= set(allowed.dimensions):
                    raise ValueError("Dimension metadata exceeds capabilities")
                if not set(metadata.metrics) <= set(allowed.metrics):
                    raise ValueError("Metric metadata exceeds capabilities")
        return self


class PlanResponse(StrictModel):
    status: Literal["query_plan"]
    plan: dict[str, Any]


class ClarificationResponse(StrictModel):
    status: Literal["needs_clarification"]
    question: str = Field(min_length=1, max_length=300)
    reasonCode: Literal["MISSING_DATE_RANGE", "AMBIGUOUS_REQUEST", "MISSING_INFORMATION"]

    @field_validator("question")
    @classmethod
    def brief_question(cls, value: str) -> str:
        if not value.strip() or any(ord(char) < 32 for char in value):
            raise ValueError("Invalid clarification question")
        return value.strip()


class UnsupportedResponse(StrictModel):
    status: Literal["unsupported"]
    message: str = Field(min_length=1, max_length=300)
    reasonCode: Literal[
        "DATASET_NOT_AVAILABLE", "CAPABILITY_NOT_AVAILABLE", "UNRECOGNIZED_REQUEST",
        "PERSONAL_IDENTIFIER_NOT_ALLOWED"
    ]


PlannerResponse = Annotated[
    PlanResponse | ClarificationResponse | UnsupportedResponse,
    Field(discriminator="status"),
]
response_adapter = TypeAdapter(PlannerResponse)

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
                        {"department.id", "department.name", "attendance.work_date"},
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


def validate_authorized_plan(request: PlannerRequest, plan: dict[str, Any]) -> None:
    """Defense in depth; Spring remains the definitive QueryPlan validator."""
    allowed_keys = {"version", "catalogVersion", "dataset", "mode", "asOfDate",
                    "dateRange", "select", "metrics", "filters", "groupBy", "having",
                    "orderBy", "page", "visualization"}
    if set(plan) - allowed_keys or plan.get("version") != "1" or plan.get("catalogVersion") != "1":
        raise ValueError("Invalid plan envelope")
    dataset = plan.get("dataset")
    caps = request.capabilities.get(dataset) if isinstance(dataset, str) else None
    mode = plan.get("mode")
    if caps is None or mode not in {"detail", "aggregate"} or not getattr(caps, mode):
        raise ValueError("Unavailable dataset or mode")
    dimensions, metrics = set(caps.dimensions), set(caps.metrics)
    metadata = request.semanticContext.datasets.get(dataset) if request.semanticContext else None
    if metadata:
        dimensions &= set(metadata.dimensions)
        metrics &= set(metadata.metrics)
    def listed(key: str) -> list:
        values = plan.get(key, [])
        if not isinstance(values, list):
            raise ValueError("Invalid plan list")
        return values
    if any(not isinstance(value, str) or value not in dimensions
           for key in ("select", "groupBy") for value in listed(key)):
        raise ValueError("Unavailable dimension")
    if any(not isinstance(value, str) or value not in metrics for value in listed("metrics")):
        raise ValueError("Unavailable metric")
    for value in listed("filters"):
        if not isinstance(value, dict) or set(value) != {"field", "op", "value"} or value["field"] not in dimensions:
            raise ValueError("Unavailable filter")
        if metadata and value["op"] not in metadata.dimensions[value["field"]].operators:
            raise ValueError("Unavailable operator")
    for value in listed("having"):
        if not isinstance(value, dict) or value.get("metric") not in metrics:
            raise ValueError("Unavailable HAVING metric")
    for value in listed("orderBy"):
        if not isinstance(value, dict) or value.get("field") not in dimensions | metrics:
            raise ValueError("Unavailable sort field")
    visualization = plan.get("visualization", {})
    if not isinstance(visualization, dict):
        raise ValueError("Invalid visualization")
    if visualization.get("x") is not None and visualization["x"] not in dimensions:
        raise ValueError("Unavailable chart dimension")
    if visualization.get("y") is not None and visualization["y"] not in metrics:
        raise ValueError("Unavailable chart metric")
    if "dateRange" in plan:
        date_range = plan["dateRange"]
        if not isinstance(date_range, dict) or date_range.get("field") not in dimensions:
            raise ValueError("Unavailable date field")


def safe_response(request: PlannerRequest, response: Any) -> PlannerResponse:
    try:
        parsed = response_adapter.validate_python(response)
        if isinstance(parsed, PlanResponse):
            validate_authorized_plan(request, parsed.plan)
        return parsed
    except (ValidationError, ValueError, TypeError) as error:
        raise HTTPException(status_code=502, detail="Invalid planner response") from error


@app.post("/internal/v1/plan", response_model=PlannerResponse)
def plan(request: PlannerRequest, _auth: Annotated[None, Depends(service_auth)]):
    mode = os.getenv("AI_PLANNER_MODE", "mock")
    if mode == "mock":
        if os.getenv("APP_ENV") not in {"development", "test"}:
            raise HTTPException(status_code=503, detail="Planner unavailable")
        return safe_response(request, mock_plan(request))
    if mode != "gemini":
        raise HTTPException(status_code=503, detail="Planner unavailable")
    try:
        return safe_response(request, GeminiPlanner().plan(request))
    except PlannerFailure as failure:
        raise HTTPException(status_code=failure.status_code, detail=failure.message) from failure
