
1. grpc服务列表
grpcurl -plaintext 127.0.0.1:9999 list


2. grpc服务测试
grpcurl -plaintext -d "{\"name\":\"Jack\"}" 127.0.0.1:9999 shop.protobuf.PingSer
vice.ping