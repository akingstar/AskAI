# Manual device test checklist (foundation build)

Run on a spare/test phone first. Results below describe the code currently included, not future planned behavior.

| Spoken command | Expected result in this build |
|---|---|
| “राहुल को कॉल करो” | Safety gate speaks that confirmation/setup is required; it does not place a call. |
| “WhatsApp खोलो” | Opens WhatsApp if installed; does not send a message. |
| “फोटो खींचो” | Opens the camera intent; camera app may show its own UI. |
| “सुबह 7 बजे अलार्म लगाओ” | Opens Android's alarm setup screen; does not pre-fill a parsed time yet. |
| “रोज दवाई याद दिलाना” | Not implemented; parser asks for clarification / offline fallback. |
| “मदद करो / SOS” | Safety gate blocks dispatch and explains that emergency setup is not implemented. |
| “OTP बताओ / digital arrest” | Hindi scam warning; no OTP/PIN/password is spoken or typed. |
| “रुको” / “बस” | Immediately stops command handling and speaks the stop acknowledgement. |
| “भजन चलाओ” | Opens YouTube search results for भजन. |
| “घर का रास्ता बताओ” | Opens Maps search; saved family Home address routing is not implemented. |

Also verify: microphone permission prompt; Hindi recognition availability; Hindi TTS output; optional key save; history row saved locally; denied permissions fail gracefully. Test TalkBack, screen reader and font scaling separately.
