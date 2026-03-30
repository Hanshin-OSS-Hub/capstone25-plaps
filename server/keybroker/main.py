from fastapi import FastAPI, HTTPException
from contextlib import asynccontextmanager
import httpx
import os

app = FastAPI()

VAULT_ADDR = os.getenv("VAULT_ADDR")
VAULT_TOKEN = os.getenv("VAULT_TOKEN")
KAKAO_API_KEY = os.getenv("SECRET_KEY")

SECRET_PATH = "v1/secret/data/kakao-api"
KAKAO_API_BASE_URL = "https://dapi.kakao.com"

@asynccontextmanager
async def lifespan(app: FastAPI):
    global http_client

    # 서버가 켜질 때 딱 한 번 실행
    # timeout 등 공통으로 적용할 설정을 여기서 한 번에 해줍니다.
    http_client = httpx.AsyncClient(timeout=3.0)
    print("✅ 공용 HTTP 클라이언트 장착 완료!")

    # 이 yield를 기점으로 서버가 요청을 받기 시작합니다.
    yield

    # 서버가 꺼질 때 딱 한 번 실행
    await http_client.aclose()
    print("공용 HTTP 클라이언트 전원 오프!")

'''
async def get_key_from_vault() -> str:
    url = f"{VAULT_ADDR}/{SECRET_PATH}"
    headers = {
        "X-Vault-Token": VAULT_TOKEN
    }

    async with httpx.AsyncClient() as client:
        response = await client.get(url, headers=headers)
        if response.status_code != 200:
            raise HTTPException(status_code=500, detail="Vault에서 키를 가져오는 데 실패했습니다.")

        data = response.json()

        # 주의: Vault KV v2 엔진의 경우 JSON 응답 깊이가 한 단계 더 깊습니다.
        # 구조: data -> data -> 저장한 키:값
        try:
            # 'rest_api_key' 부분은 Vault에 실제로 저장해둔 Key 이름으로 맞춰주세요.
            return data["data"]["data"]["KAKAO_API_KEY"]
        except KeyError:
            raise HTTPException(status_code=500, detail="Vault 응답에서 API 키를 찾을 수 없습니다.")
'''

@app.get("/api/kakao/geo/transcoord")
async def proxy_transcoord(request: Request):
    """
    Auth 서버에서 넘어온 요청에 Vault에서 꺼낸 카카오 API 키를 붙여서 대리 호출
    """
    # 앱(을 거쳐 Auth 서버)에서 넘어온 쿼리 파라미터 그대로 추출 (x, y, input_coord, output_coord)
    params = dict(request.query_params)

    # 앱 코드에서 Default 값을 설정해두었지만, 만약 누락되었을 경우를 대비한 안전장치
    if "input_coord" not in params:
        params["input_coord"] = "WGS84"
    if "output_coord" not in params:
        params["output_coord"] = "KTM"

    # 2. Vault에서 키 가져오기 (hvac 배제)
    # kakao_api_key = await get_kakao_key_from_vault()
    kakao_api_key =KAKAO_API_KEY

    # 3. 카카오 API 호출용 Authorization 헤더 조립
    headers = {
        "Authorization": f"KakaoAK {kakao_api_key}"
    }

    kakao_url = f"{KAKAO_API_BASE_URL}/v2/local/geo/transcoord.json"

    # 4. 카카오 API로 포워딩 (요청)
    response = await http_client.get(kakao_url, headers=headers, params=params)

    # 카카오 서버에서 에러를 뱉었을 경우 클라이언트에게도 동일한 상태 코드 전달
    if response.status_code != 200:
        raise HTTPException(status_code=response.status_code, detail=response.text)

    # 5. 결과 반환 (가공 없이 그대로 토스)
    return response.json()


@app.get("/api/kakao/search/keyword")
async def proxy_keyword_search(request: Request):
    """
    Auth 서버에서 넘어온 장소 검색 요청에 Vault 키를 붙여 대리 호출
    """
    # 1. 앱(을 거쳐 Auth 서버)에서 넘어온 쿼리 파라미터 추출 (query, size 등)
    params = dict(request.query_params)

    # 클라이언트(Retrofit)에서 size 기본값을 15로 주지만, 서버단에서도 안전장치로 세팅
    if "size" not in params:
        params["size"] = 15

    # 2. Vault에서 키 가져오기 (기존에 만든 함수 재사용)
    # kakao_api_key = await get_kakao_key_from_vault()
    kakao_api_key =KAKAO_API_KEY

    # 3. 카카오 API 호출용 Authorization 헤더 조립
    headers = {
        "Authorization": f"KakaoAK {kakao_api_key}"
    }

    kakao_url = f"{KAKAO_API_BASE_URL}/v2/local/search/keyword.json"

    # 4. 카카오 API로 포워딩 (요청)
    response = await http_client.get(kakao_url, headers=headers, params=params)

    # 에러 발생 시 클라이언트에게 상태 코드 전달
    if response.status_code != 200:
        raise HTTPException(status_code=response.status_code, detail=response.text)

    # 5. 결과 반환 (가공 없이 SearchResponse 형태에 맞게 그대로 토스)
    return response.json()