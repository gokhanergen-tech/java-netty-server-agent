import asyncio
from fastapi import FastAPI
from pydantic import BaseModel

from agent import analyse_users
from llama_message_agent import generate_text
from text_to_image import generate_image

app = FastAPI()

class User(BaseModel):
    name: str
    messageType: str
    message: str
    id: str

@app.post("/api/v1/agent")
async def process_user(user: User):
    print("Request")
    toxic_control = analyse_users(user.message)
    if user.messageType == "TEXT":
        result = {"response":toxic_control}
    elif user.messageType == "PROMPT":
        result = await asyncio.to_thread(generate_image, toxic_control)
    elif user.messageType == "LAMA":
        result = await asyncio.to_thread(generate_text, toxic_control)
        result = {"response":result}
    else:
        result = -1
    return result


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("server_fast_api:app", host="0.0.0.0", port=9100, reload=False)
