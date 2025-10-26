from transformers import pipeline
import random

toxic_classifier = pipeline("text-classification", model="unitary/toxic-bert")

def analyse_users(message):
    result = toxic_classifier(message)[0]
    
    print(result["score"])
    
    if result["score"] > 0.2:
        peaceful_messages = [
            "Let's treat each other with respect.",
            "We're all human — let's be kind.",
            "Please speak with compassion.",
            "Let's keep the conversation positive and respectful."
        ]
        return random.choice(peaceful_messages)
    else:
        return message

# Örnek kullanım
if __name__ == "__main__":
    messages = [
        "You are so dumb!",
        "I love this weather.",
        "Go get drunk!",
        "Have a nice day!"
    ]

    for msg in messages:
        print(f"Message: {msg}")
        print(f"Response: {analyse_users(msg)}\n")
