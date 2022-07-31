#!/bin/bash
for i in {01..45}
do
   echo "copy to dc$i.utdallas.edu"
   scp $HOME/.ssh/id_rsa.pub mea130130@dc$i.utdallas.edu:$HOME/.ssh/authorized_keys
done

