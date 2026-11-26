# Ask AI Cloudflare Worker

Create a Cloudflare KV namespace named `askai-rate-limit`, then replace the KV namespace ID in `wrangler.toml`.

Install Wrangler in Termux with `npm install -g wrangler`.
Run `wrangler login`.
From this directory run `wrangler kv namespace create RATE_LIMIT`.
Set the returned ID in `wrangler.toml`.
Set secrets:
`wrangler secret put GEMINI_API_KEY`
`wrangler secret put GROQ_API_KEY`
Deploy with `wrangler deploy`.

Copy the Worker URL into Ask AI's AI Worker URL field.

The Worker rate limits by IP and minute using KV. Free plan limits and provider quotas apply; free AI is not guaranteed to be unlimited.


## Worker tests

Run the Worker unit tests locally from the repository root:

`node --test worker/worker.test.js`

GitHub Actions runs these tests before the Android unit tests and debug APK build.
