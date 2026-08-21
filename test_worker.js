

async function test() {
    const res = await fetch("https://staybuddy-worker.aasavchauhan.workers.dev/api/send-notification", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
            targetUserId: "fake-user-id",
            title: "Test",
            body: "Test Body"
        })
    });
    console.log(res.status);
    console.log(await res.text());
}
test();
