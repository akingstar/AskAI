const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type"
};

function response(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { ...cors, "Content-Type": "application/json" }
  });
}

export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") return new Response(null, { headers: cors });
    if (request.method !== "POST") return response({ error: "POST required" }, 405);

    const ip = request.headers.get("CF-Connecting-IP") || "unknown";
    const now = Math.floor(Date.now() / 60000);
    const key = `rate:${ip}:${now}`;
    const count = Number(await env.RATE_LIMIT.get(key) || "0");
    if (count >= 20) return response({ error: "Rate limit exceeded" }, 429);
    await env.RATE_LIMIT.put(key, String(count + 1), { expirationTtl: 120 });

    const length = Number(request.headers.get("Content-Length") || "0");
    if (length > 6000) return response({ error: "Request too large" }, 413);

    let body;
    try { body = await request.json(); } catch { return response({ error: "Invalid JSON" }, 400); }
    if (typeof body.text !== "string" || body.text.length < 1 || body.text.length > 3000) {
      return response({ error: "Invalid text" }, 400);
    }

    const system = `You are Ask AI, a safe Hindi-first Android phone assistant. Understand the user's language and reply in that language. Return JSON only, with exactly this schema: {"language":"hi-IN","steps":[{"action":"open_app","target":"YouTube","value":""}]}. Root keys are language and steps. Each step has action,target,value. At most five steps. Never invent a phone number. You receive app names and contact display names only, never phone numbers. Never perform payments, UPI, OTP, PIN, password, factory reset or destructive actions. If uncertain, return {"language":"hi-IN","steps":[]}. No markdown.`;
    const prompt = `${system}\nUser command: ${body.text}`;

    let result = await gemini(env, prompt);
    if (!result) result = await groq(env, prompt);
    if (!result) return response({ error: "AI providers unavailable" }, 503);

    try {
      const parsed = JSON.parse(result);
      if (!parsed || typeof parsed.language !== "string" ||
          !Array.isArray(parsed.steps) || parsed.steps.length > 5) throw new Error("schema");
      for (const step of parsed.steps) {
        if (!step || typeof step.action !== "string" ||
            typeof step.target !== "string" || typeof step.value !== "string") throw new Error("step");
      }
      return response({ result: JSON.stringify(parsed) });
    } catch {
      return response({ error: "Invalid model response" }, 502);
    }
  }
};

async function gemini(env, prompt) {
  if (!env.GEMINI_API_KEY) return null;
  try {
    const r = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=${env.GEMINI_API_KEY}`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          contents: [{ parts: [{ text: prompt }] }],
          generationConfig: { temperature: 0.1, responseMimeType: "application/json" }
        })
      }
    );
    if (!r.ok) return null;
    const data = await r.json();
    return data.candidates?.[0]?.content?.parts?.[0]?.text || null;
  } catch { return null; }
}

async function groq(env, prompt) {
  if (!env.GROQ_API_KEY) return null;
  try {
    const r = await fetch("https://api.groq.com/openai/v1/chat/completions", {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${env.GROQ_API_KEY}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        model: "llama-3.1-8b-instant",
        temperature: 0.1,
        messages: [
          { role: "system", content: "Return only JSON. " + prompt.split("\nUser command:")[0] },
          { role: "user", content: prompt.split("\nUser command:")[1] || prompt }
        ]
      })
    });
    if (!r.ok) return null;
    const data = await r.json();
    return data.choices?.[0]?.message?.content || null;
  } catch { return null; }
}
