# istio-log-parsing

```bash
k logs deploy/cms istio-proxy > cms.test.log
k logs deploy/cms-client istio-proxy > cms-client.test.log
k logs deploy/ems istio-proxy > ems.test.log
k logs deploy/ems-client istio-proxy > ems-client.test.log
k logs deploy/qms istio-proxy > qms.test.log
k logs deploy/qms-client istio-proxy > qms-client.test.log
k logs deploy/ums istio-proxy > ums.test.log
k logs deploy/ums-client istio-proxy > ums-client.test.log
k logs sts/postgres istio-proxy > postgres.test.log
k logs deploy/keycloak istio-proxy > keycloak.test.log
k logs -n istio-system deploy/istio-ingressgateway > istio-ingressgateway.istio-system.log
```