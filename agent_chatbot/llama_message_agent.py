
import ollama

client = ollama.Client()
def generate_text(prompt: str) -> str:
    print(f"Received prompt: {prompt}")
  
    # Eğer model "You are a helpful assistant..." kısmını tekrar ediyorsa kırpıyoruz
    response= client.generate(
        model="llama3.2:3b",
        prompt=prompt
    )
    return response.response
