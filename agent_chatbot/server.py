import asyncio
import base64
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
            # read one line instead of raw bytes
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

            response_message = None

            if process_type == "TEXT":
                # Agent işlemi simüle et
                response_message = analyse_users(message)
            elif process_type == "PROMPT":
                # Base64 image handling
                try:
                    image_data = generate_image(message)
        
                    response_message = image_data
                except Exception as e:
                    response_message = -1
            else:
                response_message = -1

            writer.write(f"{response_message}\n".encode())
            await writer.drain()

    except Exception as e:
        print(f"Error: {e}")
    finally:
        writer.close()
        await writer.wait_closed()
        print(f"{addr} connection closed.")

async def main():
    server = await asyncio.start_server(handle_client, HOST, PORT)
    print(f"Agent server is working...")
    async with server:
        await server.serve_forever()

asyncio.run(main())
