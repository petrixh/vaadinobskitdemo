#!/bin/sh
echo "Installing NewRelic Infra Host monitor in docker image... If you have changed the configs or want a newer version of the agent you should force a rebuild of the image..."
echo "Do you want to force a rebuild of the docker image? y/N?"
select yn in "Yes" "No"; do
  case $yn in
    Yes ) docker-compose up -d --build
    No ) docker-compse up -d
  esac
done
