pragma solidity ^0.4.24;

 contract token{
     constructor() public payable{}

     // 4）suicide也会转移token
     // 所有token，trx均被转移到toAddress,
     // 若toAddress为合约地址本身，则所有token，trx均被烧掉进黑洞
     function kill(address toAddress) payable public{
         selfdestruct(toAddress);
     }

 }

contract B{
    constructor() public payable {}
    function() public payable {}
}