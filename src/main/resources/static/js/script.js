 async function resendToken(token) {
    try {
        const response = await fetch(`/resendVerifyToken?token=${token}`, {
            method: 'GET',
        });
        const data = await response.json();
        if (response.ok) {
            alert("Verification token resent successfully. Please check your email.");
        } else {
            alert("Failed to resend verification token: " + data.message);
        }
    } catch (error) {
        alert("An error occurred while resending the verification token.");
    }
}