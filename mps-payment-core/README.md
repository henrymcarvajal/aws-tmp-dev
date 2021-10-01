
# mps-payment-core
Core of MPS

remote QA name in heroku: qa appName: mpscore
remote production name in heroku: production appName: mpscore-prod

branch for qa deploying: MPS-development
branch for production deployment: master

git commands:

* Deploying to qa a specific branch to qa application

git push qa branchName:master

*Deploying to  production from master branch

git push production master

# run local

./mvnw spring-boot:run -Dspring-boot.run.profiles=local

* Connect to machine of app

heroku run bash --app mpscore



