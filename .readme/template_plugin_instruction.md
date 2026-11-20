{{ instruction_p_what }}

### {{ instruction_h3_start }}

{{ placeholder_instruction_start_steps }}

### {{ instruction_h3_features }}

{{ placeholder_instruction_features }}

### {{ instruction_h3_script }}

{{ instruction_p_script }}

```js
let wake = speech.wake(["hey macaw", "小六小六"], { thenListen: true });
wake.on("wake", e => {
    e.listen.on("final", r => {
        console.log(r.text);
        speech.speak(r.text);
    });
});
```

{{ placeholder_instruction_script_notes }}

### {{ instruction_h3_requirements }}

{{ placeholder_instruction_requirements }}

### {{ instruction_h3_faq }}

{{ placeholder_instruction_faq }}

{{ instruction_p_more }}
