pragma solidity >=0.4.22 <0.6.0;

import "./SafeMath.sol";
import "./Ownable.sol";

contract TronHi {
    function balanceOf(address account) external view returns (uint256);

    function transferByMinter(address account, address recipient, uint256 amount) public returns (bool);

    function burnByMinter(address account, uint256 amount) public returns (bool);
}

contract Queue
{
    using SafeMath for uint256;

    uint256 totalBurnt = 0;
    uint256 totalAuction = 0;

    TronHi internal tronHi;

    struct AuctionQueue {
        address[10] better;
        uint256[10] amount;
        uint256 front;
        uint256 rear;
    }

    function pushQueue(AuctionQueue storage q, address _better, uint256 _amount) internal
    {
        require(tronHi.balanceOf(_better) >= _amount, "better's balance is not sufficient.");
        tronHi.burnByMinter(_better, _amount.div(2));
        tronHi.transferByMinter(_better, address(this), _amount.div(2));
        totalBurnt += _amount.div(2);

        // popQueue /////////////////////////////////////////////////////////
        if ((q.rear + 1) % q.better.length == q.front) {
            if (q.rear == q.front)
                return;
            address better = q.better[q.front];
            uint256 amount = q.amount[q.front];
            delete q.better[q.front];
            delete q.amount[q.front];
            q.front = (q.front + 1) % q.better.length;

            tronHi.transferByMinter(address(this), better, amount.div(2));
        }
        ////////////////////////////////////////////////////////////
        q.better[q.rear] = _better;
        q.amount[q.rear] = _amount;
        q.rear = (q.rear + 1) % q.better.length;
    }

    function popQueue(AuctionQueue storage q) internal
    {
        if (q.rear == q.front)
            return;
        tronHi.burnByMinter(address(this), q.amount[q.front].div(2));
        totalBurnt += q.amount[q.front].div(2);

        delete q.better[q.front];
        delete q.amount[q.front];
        q.front = (q.front + 1) % q.better.length;
    }

    function isEmpty(AuctionQueue storage q) view internal returns (bool){
        return (q.rear == q.front);
    }

}

contract Auction is Queue, Ownable {

    event AuctionEvent(
        uint256 round,
        address bettor,
        uint256 upAmount,
        uint256 time
    );

    event AuctionResultEvent(
        uint256 round,
        address[10] bettor,
        uint256[10] upAmount,
        uint256 amount,
        uint256 time
    );

    modifier isHuman() {
        address _addr = msg.sender;
        require(_addr == tx.origin);

        uint256 _codeLength;

        assembly {_codeLength := extcodesize(_addr)}
        require(_codeLength == 0, "sorry humans only");
        _;
    }

    AuctionQueue auctionQueue;
    uint256 deadline = 0;
    uint256 round = 1;

    constructor () public {}

    function toAuction(uint256 _amount) public isHuman returns (uint256) {
        require(deadline > now, "deadline > now.");
        require((_amount - getMin()) >= 1000000, "bit amount need greater before.");
        pushQueue(auctionQueue, msg.sender, _amount);
        if (deadline < (now + 5 minutes)) {
            deadline = now + 5 minutes;
        }
        emit AuctionEvent(round, msg.sender, _amount, deadline);
        return deadline;
    }

    function landAuction(uint256 _trxAmount) public onlyOwner returns (bool) {
        require(deadline <= now, "auctionTime < now.");
        while (!isEmpty(auctionQueue))
            popQueue(auctionQueue);

        emit AuctionResultEvent(round, auctionQueue.better, auctionQueue.amount, _trxAmount, deadline);

        totalAuction += _trxAmount;
        return true;
    }

    function startAuction(bool isActivity) public {
        round += 1;
        if (isActivity)
            deadline = now + 1 hours;
        deadline = now + 5 minutes;
    }

    function resetAuction(bool _isReset) public {
        if (_isReset) {
            round = 1;
            totalAuction = 0;
            totalBurnt = 0;
        }
        for (uint8 i = 0; i < auctionQueue.better.length; i++) {
            auctionQueue.better[i] = address(0);
            auctionQueue.amount[i] = 0;
        }
        auctionQueue.front = auctionQueue.rear = 0;
    }

    function getRoundMsg() public view returns (uint256 front, uint256 rear, address[10] memory _betterArrary, uint256[10] memory _amountArrary, uint256 _deadline, uint256 _round, uint256 _max) {
        return (auctionQueue.front, auctionQueue.rear, auctionQueue.better, auctionQueue.amount, deadline, round, getMax());
    }

    function getAuctionMsg() public view returns (uint256 _totalAuction, uint256 _totalBurnt) {
        return (totalAuction, totalBurnt);
    }

    function getMax() public view returns (uint256) {
        if (auctionQueue.rear == 0)
            return auctionQueue.amount[auctionQueue.better.length - 1];
        return auctionQueue.amount[auctionQueue.rear - 1];
    }

    function getMin() public view returns (uint256) {
        return auctionQueue.amount[auctionQueue.front];
    }

    function setInterface(address _tronHi) public onlyOwner returns (bool) {
        tronHi = TronHi(_tronHi);
        return true;
    }

}
