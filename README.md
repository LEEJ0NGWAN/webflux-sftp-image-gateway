# webflux-sftp-image-gateway

This is an sample image data uploader.

It decodes Base64-encoded image data in request body json and uploads decoded bytes to a target remote SFTP Server.

### Reactive Programming

It uses webflux(reactor streams and netty) for an asnyc flow.

### SFTP Connection Pool

Opening SSH Connection is so expensive that we should avoid opening a new SSH connection per each upload request.

To prevent opening a new SSH connection per each request, it implements SFTP connection pool in memory with concurrent hash map collections.

### Flow

![flow](./image-gateway.png)

### Specification
- maven
- Spring Boot Webflux (2.6.6)
- [jsch 0.2.7(unofficial)](https://github.com/mwiede/jsch)
