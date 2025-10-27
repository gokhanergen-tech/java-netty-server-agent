import asyncio
import json
from agent import analyse_users
from text_to_image import generate_image

HOST = '127.0.0.1'  # Tüm IP'lerden dinle
PORT = 9100       # Agent socket server portu

async def handle_client(reader, writer):
    addr = writer.get_extra_info('peername')
    print(f"Connection: {addr}")

    try:
        while True:

            data = await reader.readline()

            if not data:
                print("Connection closed by client.")
                break
            

            decoded = data.decode('utf-8').strip()
            if not decoded:
                print("Empty string received.")
                continue

            try:
                _user = json.loads(decoded)
            except json.JSONDecodeError:
                print(f"Invalid JSON received: {decoded}")
                continue

            process_type = _user.get("messageType")
            message = _user.get("message")

            if process_type == "TEXT":
                asyncio.create_task(process_text(writer, message))
            elif process_type == "PROMPT":
                asyncio.create_task(process_prompt(writer, message))
            else:
                writer.write(b"-1\n")
                await writer.drain()

    except Exception as e:
        print(f"Error: {e}")
    finally:
        writer.close()
        await writer.wait_closed()
        print(f"{addr} connection closed.")
        
async def process_text(writer, message):
    try:
        result = await asyncio.to_thread(analyse_users, message)
    except Exception:
        result = -1
    writer.write(f"{result}\n".encode())
    await writer.drain()

async def process_prompt(writer, message):
    try:
        result = await asyncio.to_thread(generate_image, message)
    except Exception:
        result = -1
    writer.write(f"{result}\n".encode())
    await writer.drain()

async def main():
    server = await asyncio.start_server(handle_client, HOST, PORT)
    print(f"Agent server is working...")
    async with server:
        await server.serve_forever()

asyncio.run(main())
