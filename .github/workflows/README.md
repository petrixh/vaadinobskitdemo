# Test locally

Install act: 

```
curl --proto '=https' --tlsv1.2 -sSf https://raw.githubusercontent.com/nektos/act/master/install.sh | sudo bash
```

Find jobs: 

```
./bin/act --list
```



Secrets can be added with: 

```
--secret MY_SECRET=secretvalue
```

Env variables with: 

```
act --env MY_ENV=value
```

Run a job: 

```
./bin/act --env PLAYWRIGHT_SKIP_VALIDATE_HOST_REQUIREMENTS=true push
```

