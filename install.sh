#!/bin/bash

set -e 

function check_command() {
    if ! command -v "$1" &> /dev/null; then
        echo "$1 is not installed, please install it before proceeding."
        exit 1
    fi
}

check_command java
check_command mvn
check_command git

if [ -d "BuildCLI/.git" ]; then
    echo "Project directory already exists. Pulling latest changes..."
    cd BuildCLI
    git pull origin main || { echo "Failed to update repository."; exit 1; }
else
    echo "Cloning repository..."
    if ! git clone https://github.com/BuildCLI/BuildCLI.git ; then 
        echo "Failed to clone repository. Make sure Git is installed."
        exit 1
    fi
    cd BuildCLI
fi

if ! mvn clean package; then
    echo "Error while creating Maven package."
    exit 1
fi

if [ ! -d "$HOME/bin" ]; then
    mkdir -p "$HOME/bin"
fi

cp target/buildcli.jar "$HOME/bin/"

cat <<EOF > "$HOME/bin/buildcli"
#!/bin/bash
java -jar "\$HOME/bin/buildcli.jar" "\$@"
EOF

chmod +x "$HOME/bin/buildcli"

if [[ ":$PATH:" != *":$HOME/bin:"* ]]; then
    echo "The directory \$HOME/bin is not in the PATH."
    echo "Please add the following line to your ~/.bashrc, ~/.zshrc, or the appropriate shell configuration file:"
    echo ""
    echo 'export PATH="$HOME/bin:$PATH"'
    echo ""
    echo "Then, reload your shell with:"
    echo "source ~/.bashrc  # or source ~/.zshrc if you use Zsh"
    echo ""
    echo "After that, you can run the application with: buildcli"
else 
    echo "You can now run the application with: buildcli"
fi
