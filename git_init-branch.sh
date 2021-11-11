#!/bin/bash
#########################################################################
# Copyright (C) 2021 Akito <the@akito.ooo>                              #
#                                                                       #
# This program is free software: you can redistribute it and/or modify  #
# it under the terms of the GNU General Public License as published by  #
# the Free Software Foundation, either version 3 of the License, or     #
# (at your option) any later version.                                   #
#                                                                       #
# This program is distributed in the hope that it will be useful,       #
# but WITHOUT ANY WARRANTY; without even the implied warranty of        #
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the          #
# GNU General Public License for more details.                          #
#                                                                       #
# You should have received a copy of the GNU General Public License     #
# along with this program.  If not, see <http://www.gnu.org/licenses/>. #
#########################################################################


# http://redsymbol.net/articles/unofficial-bash-strict-mode/
set -o errexit
set -o nounset
set -o pipefail

function getCurrentBranch { git rev-parse --abbrev-ref HEAD 2>/dev/null; }
function getCurrentRemote { git config --get branch.$current_branch.remote; }
function errOut { echo "Something went wrong!"; }

trap errOut ERR

provided_branch="$1"
if [[ -z "${provided_branch}" ]]; then
  echo "Please provide the Name of the Branch you want to create!"
  exit 1
fi
current_branch="$(getCurrentBranch)"
current_remote="$(getCurrentRemote)"
echo "Create new Branch"
echo "=============================================="
git checkout -b "${provided_branch}"
echo "=============================================="
echo "=============================================="
echo
wait
current_branch="$(getCurrentBranch)"
echo "Push new Branch to default Remote"
echo "=============================================="
git push "${current_remote}" "${current_branch}"
echo "=============================================="
echo "=============================================="
echo
echo "Setting Remote counter-part of this Local Branch as Upstream"
echo "=============================================="
git branch --set-upstream-to="${current_remote}/${current_branch}" "${current_branch}"
echo "=============================================="
echo "=============================================="
echo
echo "Done!"