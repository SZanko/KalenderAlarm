// The first browser test also loads the time zone database (@js-joda/timezone),
// which can exceed Mocha's default 2 s timeout on a cold headless Chrome.
config.set({
    client: Object.assign({}, config.client, {
        mocha: Object.assign({}, (config.client || {}).mocha, { timeout: 30000 }),
    }),
});
