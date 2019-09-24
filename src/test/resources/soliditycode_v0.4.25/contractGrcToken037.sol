pragma solidity ^0.4.24;

contract transferGrc10 {
    function receive(address rec) public payable {
        uint256 aamount=address(this).tokenBalance(msg.tokenid);
        uint256 bamount=rec.tokenBalance(msg.tokenid);
        require(msg.tokenvalue==aamount);
        require(aamount==msg.tokenvalue);
        rec.transferToken(aamount,msg.tokenid);
        require(0==address(this).tokenBalance(msg.tokenid));
        require(bamount+aamount==rec.tokenBalance(msg.tokenid));
        require(rec.call(bytes4(keccak256("checkGrc10(uint256,grcToken,uint256)")),bamount+aamount,msg.tokenid,0));
    }
}

contract receiveGrc10 {
    function() public payable {
    }
    function checkGrc10(uint256 amount,grcToken tid,uint256 meamount) public{
        require(amount==address(this).tokenBalance(tid));
        require(meamount==msg.sender.tokenBalance(tid));
    }
}