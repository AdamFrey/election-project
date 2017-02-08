build:
	boot prod

deploy: export ASSET_HOST=elections/
deploy: export GOOGLE_MAPS_API_KEY=AIzaSyCRTgKAeU6LuBJRYKWWi8O6uVCsexO3Jm8
deploy: build
