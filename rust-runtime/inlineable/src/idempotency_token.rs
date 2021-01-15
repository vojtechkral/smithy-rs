/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

use std::sync::Mutex;

pub(crate) fn uuid_v4(input: u128) -> String {
    let mut out = String::with_capacity(36);
    // u4-aligned index into [input]
    let mut rnd_idx: u8 = 0;
    const HEX_CHARS: &[u8; 16] = b"0123456789abcdef";

    for str_idx in 0..36 {
        if str_idx == 8 || str_idx == 13 || str_idx == 18 || str_idx == 23 {
            out.push('-');
        // UUID version character
        } else if str_idx == 14 {
            out.push('4');
        } else {
            let mut dat: u8 = ((input >> (rnd_idx * 4)) & 0x0F) as u8;
            // UUID variant bits
            if str_idx == 19 {
                dat |= 0b00001000;
            }
            rnd_idx += 1;
            out.push(HEX_CHARS[dat as usize] as char);
        }
    }
    out
}

pub trait ProvideIdempotencyToken {
    fn token(&self) -> String;
}

pub fn default_provider() -> impl ProvideIdempotencyToken {
    Mutex::new(rand::thread_rng())
}

impl<T> ProvideIdempotencyToken for Mutex<T>
where
    T: rand::Rng,
{
    fn token(&self) -> String {
        let input: u128 = self.lock().unwrap().gen();
        uuid_v4(input)
    }
}

impl ProvideIdempotencyToken for &'static str {
    fn token(&self) -> String {
        self.to_string()
    }
}