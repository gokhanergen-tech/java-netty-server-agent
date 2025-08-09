import asyncio
import json
from agent import analyse_users

HOST = '127.0.0.1'  # Tüm IP'lerden dinle
PORT = 9100       # Agent socket server portu

async def handle_client(reader, writer):
    addr = writer.get_extra_info('peername')
    print(f"Connection: {addr}")

    try:
        while True:
            data = await reader.read(4096)
            
            _user = json.loads(data.decode('utf-8'))
            
            if not data:
                break

            try:
                print(_user)
                message = _user["message"]
                agent_response = analyse_users(message)
                
                response_message = None
                if agent_response.__contains__("-1"):
                    response_message = message
                else:
                    response_message = agent_response
                
                print(agent_response)
                print(response_message)
                
                writer.write(f"{response_message.strip()}\n".encode())
                await writer.drain()
            except json.JSONDecodeError:
                print("Taken invalid token")
                
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
