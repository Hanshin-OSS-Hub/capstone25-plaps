from fastapi import FastAPI, Header, HTTPException
from contextlib import asynccontextmanager
import httpx
import os

TARGET_BASE_URL = os.getenv("TARGET_URL")

http_client: httpx.AsyncClient | None = None

@asynccontextmanager
async def lifespan(app: FastAPI):
    global http_client

    # 서버가 켜질 때 딱 한 번 실행
    # timeout 등 공통으로 적용할 설정을 여기서 한 번에 해줍니다.
    http_client = httpx.AsyncClient(timeout=3.0)
    print("✅ 공용 HTTP 클라이언트 장착 완료!")

    # 이 yield를 기점으로 서버가 요청을 받기 시작합니다.
    yield

    await http_client.aclose()

app = FastAPI(lifespan=lifespan)

# 장소 검색 API
@app.get("/api/search")
async def search_keyword(
    query: str,
    size: int = 15
    # authorization: str = Header(None)  <- 나중에 JWT나 소셜 로그인 토큰으로 받기.
):
    #인증 로직
    # if not authorization:
    #     raise HTTPException(status_code=401, detail="토큰이 없습니다.")

    response = await http_client.get(
        f"{TARGET_BASE_URL}/api/kakao/search/keyword",
        params={"query": query, "size": size}
    )
    return response.json()


# 좌표 변환 API
@app.get("/api/geo")
async def trans_coord(
    x: float,
    y: float,
    input_coord: str = "WGS84",
    output_coord: str = "KTM"
    # authorization: str = Header(None) <- 나중에 JWT나 소셜 로그인 토큰으로 받기.
):
    #인증 로직
    # if not authorization:
    #     raise HTTPException(status_code=401, detail="토큰이 없습니다.")


    response = await http_client.get(
        f"{TARGET_BASE_URL}/api/kakao/geo/trans_coord",
        params={
            "x": x,
            "y": y,
            "input_coord": input_coord,
            "output_coord": output_coord
        }
    )
    return response.json()