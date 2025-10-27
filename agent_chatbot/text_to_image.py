from diffusers import StableDiffusionPipeline
from fastapi.responses import StreamingResponse
import torch
from io import BytesIO

device = "cuda" if torch.cuda.is_available() else "cpu"
pipe = StableDiffusionPipeline.from_pretrained(
    "runwayml/stable-diffusion-v1-5",
    torch_dtype=torch.float16 if device=="cuda" else torch.float32
).to(device)

def generate_image(prompt):
    print(f"Received prompt: {prompt}")
    
    prompt += ", pencil drawing, black and white, sketch"
    
    image = pipe(prompt, guidance_scale=7.5).images[0]
    
    buffered = BytesIO()
    image.save(buffered, format="PNG")
    buffered.seek(0)
    return StreamingResponse(buffered, media_type="image/png")
    
    
    
    